package eu.faircode.email;

/*
    This file is part of Safe email.

    Safe email is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetGuard is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetGuard.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018 by Marcel Bokhorst (M66B)
*/

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;

public class FragmentAccount extends Fragment {
    private List<Provider> providers;

    private EditText etName;
    private Spinner spProfile;
    private EditText etHost;
    private EditText etPort;
    private EditText etUser;
    private EditText etPassword;
    private CheckBox cbPrimary;
    private CheckBox cbSynchronize;
    private Button btnOk;
    private ProgressBar pbCheck;

    static final int DEFAULT_INBOX_SYNC = 30;
    static final int DEFAULT_STANDARD_SYNC = 7;

    private static final List<String> standard_sync = Arrays.asList(
            EntityFolder.TYPE_DRAFTS,
            EntityFolder.TYPE_SENT
    );

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Get arguments
        Bundle args = getArguments();
        final long id = args.getLong("id", -1);

        // Get providers
        providers = Provider.loadProfiles(getContext());
        providers.add(0, new Provider(getString(R.string.title_custom)));

        // Get controls
        spProfile = view.findViewById(R.id.spProvider);
        etName = view.findViewById(R.id.etName);
        etHost = view.findViewById(R.id.etHost);
        etPort = view.findViewById(R.id.etPort);
        etUser = view.findViewById(R.id.etUser);
        etPassword = view.findViewById(R.id.etPassword);
        cbPrimary = view.findViewById(R.id.cbPrimary);
        cbSynchronize = view.findViewById(R.id.cbSynchronize);
        btnOk = view.findViewById(R.id.btnOk);
        pbCheck = view.findViewById(R.id.pbCheck);

        // Wire controls

        spProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Provider provider = providers.get(position);
                if (provider.imap_port != 0) {
                    etName.setText(provider.name);
                    etHost.setText(provider.imap_host);
                    etPort.setText(Integer.toString(provider.imap_port));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        ArrayAdapter<Provider> adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, providers);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spProfile.setAdapter(adapter);

        pbCheck.setVisibility(View.GONE);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnOk.setEnabled(false);
                pbCheck.setVisibility(View.VISIBLE);

                Bundle args = new Bundle();
                args.putLong("id", id);
                args.putString("name", etName.getText().toString());
                args.putString("host", etHost.getText().toString());
                args.putString("port", etPort.getText().toString());
                args.putString("user", etUser.getText().toString());
                args.putString("password", etPassword.getText().toString());
                args.putBoolean("primary", cbPrimary.isChecked());
                args.putBoolean("synchronize", cbSynchronize.isChecked());

                getLoaderManager().restartLoader(ActivityView.LOADER_ACCOUNT_PUT, args, putLoaderCallbacks).forceLoad();
            }
        });

        DB.getInstance(getContext()).account().liveAccount(id).observe(this, new Observer<EntityAccount>() {
            @Override
            public void onChanged(@Nullable EntityAccount account) {
                etName.setText(account == null ? null : account.name);
                etHost.setText(account == null ? null : account.host);
                etPort.setText(account == null ? null : Long.toString(account.port));
                etUser.setText(account == null ? null : account.user);
                etPassword.setText(account == null ? null : account.password);
                cbPrimary.setChecked(account == null ? true : account.primary);
                cbSynchronize.setChecked(account == null ? true : account.synchronize);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(R.string.title_edit_account);
    }

    private static class PutLoader extends AsyncTaskLoader<Throwable> {
        private Bundle args;

        PutLoader(Context context) {
            super(context);
        }

        void setArgs(Bundle args) {
            this.args = args;
        }

        @Override
        public Throwable loadInBackground() {
            try {
                String name = args.getString("name");
                String host = args.getString("host");
                String port = args.getString("port");
                String user = args.getString("user");

                if (TextUtils.isEmpty(name))
                    name = host + "/" + user;
                if (TextUtils.isEmpty(port))
                    port = "0";

                DB db = DB.getInstance(getContext());
                EntityAccount account = db.account().getAccount(args.getLong("id"));
                boolean update = (account != null);
                if (account == null)
                    account = new EntityAccount();
                account.name = name;
                account.host = host;
                account.port = Integer.parseInt(port);
                account.user = user;
                account.password = Objects.requireNonNull(args.getString("password"));
                account.primary = args.getBoolean("primary");
                account.synchronize = args.getBoolean("synchronize");

                // Check IMAP server
                List<EntityFolder> folders = new ArrayList<>();
                if (account.synchronize) {
                    Session isession = Session.getDefaultInstance(MessageHelper.getSessionProperties(), null);
                    IMAPStore istore = null;
                    try {
                        istore = (IMAPStore) isession.getStore("imaps");
                        istore.connect(account.host, account.port, account.user, account.password);

                        if (!istore.hasCapability("IDLE"))
                            throw new MessagingException(getContext().getString(R.string.title_no_idle));

                        boolean drafts = false;
                        for (Folder ifolder : istore.getDefaultFolder().list("*")) {
                            String[] attrs = ((IMAPFolder) ifolder).getAttributes();
                            for (String attr : attrs) {
                                if (attr.startsWith("\\")) {
                                    int index = EntityFolder.STANDARD_FOLDER_ATTR.indexOf(attr.substring(1));
                                    if (index >= 0) {
                                        EntityFolder folder = new EntityFolder();
                                        folder.name = ifolder.getFullName();
                                        folder.type = EntityFolder.STANDARD_FOLDER_TYPE.get(index);
                                        folder.synchronize = standard_sync.contains(folder.type);
                                        folder.after = DEFAULT_STANDARD_SYNC;
                                        folders.add(folder);

                                        Log.i(Helper.TAG, "Standard folder=" + folder.name +
                                                " type=" + folder.type + " attr=" + TextUtils.join(",", attrs));

                                        if (EntityFolder.TYPE_DRAFTS.equals(folder.type))
                                            drafts = true;

                                        break;
                                    }
                                }
                            }
                        }
                        if (!drafts)
                            throw new MessagingException(getContext().getString(R.string.title_no_drafts));
                    } finally {
                        if (istore != null)
                            istore.close();
                    }
                }

                if (account.primary)
                    db.account().resetPrimary();

                if (update)
                    db.account().updateAccount(account);
                else
                    try {
                        db.beginTransaction();
                        account.id = db.account().insertAccount(account);

                        EntityFolder inbox = new EntityFolder();
                        inbox.name = "INBOX";
                        inbox.type = EntityFolder.TYPE_INBOX;
                        inbox.synchronize = true;
                        inbox.after = DEFAULT_INBOX_SYNC;
                        folders.add(0, inbox);

                        for (EntityFolder folder : folders) {
                            folder.account = account.id;
                            Log.i(Helper.TAG, "Creating folder=" + folder.name + " (" + folder.type + ")");
                            folder.id = db.folder().insertFolder(folder);
                        }

                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }

                ServiceSynchronize.restart(getContext(), "account");

                return null;
            } catch (Throwable ex) {
                Log.e(Helper.TAG, ex + "\n" + Log.getStackTraceString(ex));
                return ex;
            }
        }
    }

    private LoaderManager.LoaderCallbacks putLoaderCallbacks = new LoaderManager.LoaderCallbacks<Throwable>() {
        @NonNull
        @Override
        public Loader<Throwable> onCreateLoader(int id, Bundle args) {
            PutLoader loader = new PutLoader(getActivity());
            loader.setArgs(args);
            return loader;
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Throwable> loader, Throwable ex) {
            getLoaderManager().destroyLoader(loader.getId());

            btnOk.setEnabled(true);
            pbCheck.setVisibility(View.GONE);

            if (ex == null)
                getFragmentManager().popBackStack();
            else {
                Log.w(Helper.TAG, ex + "\n" + Log.getStackTraceString(ex));
                Toast.makeText(getContext(), Helper.formatThrowable(ex), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Throwable> loader) {
        }
    };
}
