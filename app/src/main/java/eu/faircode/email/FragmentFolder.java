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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class FragmentFolder extends Fragment {
    private CheckBox cbSynchronize;
    private EditText etAfter;
    private Button btnOk;

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folder, container, false);

        // Get arguments
        Bundle args = getArguments();
        final long id = args.getLong("id");

        // Get controls
        cbSynchronize = view.findViewById(R.id.cbSynchronize);
        etAfter = view.findViewById(R.id.etAfter);
        btnOk = view.findViewById(R.id.btnOk);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnOk.setEnabled(false);

                Bundle args = new Bundle();
                args.putLong("id", id);
                args.putBoolean("synchronize", cbSynchronize.isChecked());
                args.putString("after", etAfter.getText().toString());

                getLoaderManager().restartLoader(ActivityView.LOADER_FOLDER_PUT, args, putLoaderCallbacks).forceLoad();
            }
        });

        DB.getInstance(getContext()).folder().liveFolder(id).observe(this, new Observer<EntityFolder>() {
            @Override
            public void onChanged(@Nullable EntityFolder folder) {
                if (folder != null) {
                    cbSynchronize.setChecked(folder.synchronize);
                    etAfter.setText(Integer.toString(folder.after));
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(R.string.title_edit_folder);
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
                long id = args.getLong("id");
                boolean synchronize = args.getBoolean("synchronize");
                String after = args.getString("after");
                int days = (TextUtils.isEmpty(after) ? 7 : Integer.parseInt(after));

                DB db = DB.getInstance(getContext());
                DaoFolder dao = db.folder();
                EntityFolder folder = dao.getFolder(id);
                folder.synchronize = synchronize;
                folder.after = days;
                dao.updateFolder(folder);

                if (!folder.synchronize)
                    db.message().deleteMessages(folder.id);

                ServiceSynchronize.restart(getContext(), "folder");

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
