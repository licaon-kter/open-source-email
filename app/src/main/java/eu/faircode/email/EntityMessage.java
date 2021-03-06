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

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import static android.arch.persistence.room.ForeignKey.CASCADE;

// https://developer.android.com/training/data-storage/room/defining-data

@Entity(
        tableName = EntityMessage.TABLE_NAME,
        foreignKeys = {
                @ForeignKey(childColumns = "account", entity = EntityAccount.class, parentColumns = "id", onDelete = CASCADE),
                @ForeignKey(childColumns = "folder", entity = EntityFolder.class, parentColumns = "id", onDelete = CASCADE),
                @ForeignKey(childColumns = "identity", entity = EntityIdentity.class, parentColumns = "id", onDelete = CASCADE),
                @ForeignKey(childColumns = "replying", entity = EntityMessage.class, parentColumns = "id", onDelete = CASCADE)
        },
        indices = {
                @Index(value = {"account"}),
                @Index(value = {"folder"}),
                @Index(value = {"identity"}),
                @Index(value = {"replying"}),
                @Index(value = {"folder", "uid"}, unique = true),
                @Index(value = {"thread"}),
                @Index(value = {"received"})
                // ui_seen? ui_seen?
        }
)
public class EntityMessage {
    static final String TABLE_NAME = "message";

    @PrimaryKey(autoGenerate = true)
    public Long id;
    public Long account; // performance, compose = null
    @NonNull
    public Long folder;
    public Long identity;
    public Long replying;
    public Long uid; // compose = null
    public String msgid;
    public String references;
    public String inreplyto;
    public String thread; // compose = null
    public String from;
    public String to;
    public String cc;
    public String bcc;
    public String reply;
    public String subject;
    public String body;
    public Long sent; // compose = null
    @NonNull
    public Long received; // compose = stored
    @NonNull
    public Boolean seen;
    @NonNull
    public Boolean ui_seen;
    @NonNull
    public Boolean ui_hide;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EntityMessage) {
            EntityMessage other = (EntityMessage) obj;
            return (this.account == null ? other.account == null : this.account.equals(other.account) &&
                    this.folder.equals(other.folder) &&
                    this.identity == null ? other.identity == null : this.identity.equals(other.identity) &&
                    this.replying == null ? other.replying == null : this.replying.equals(other.replying) &&
                    this.uid == null ? other.uid == null : this.uid.equals(other.uid) &&
                    this.msgid == null ? other.msgid == null : this.msgid.equals(other.msgid) &&
                    this.references == null ? other.references == null : this.references.equals(other.references) &&
                    this.inreplyto == null ? other.inreplyto == null : this.inreplyto.equals(other.inreplyto) &&
                    this.thread == null ? other.thread == null : thread.equals(other.thread) &&
                    this.from == null ? other.from == null : this.from.equals(other.from) &&
                    this.to == null ? other.to == null : this.to.equals(other.to) &&
                    this.cc == null ? other.cc == null : this.cc.equals(other.cc) &&
                    this.bcc == null ? other.bcc == null : this.bcc.equals(other.bcc) &&
                    this.reply == null ? other.reply == null : this.reply.equals(other.reply) &&
                    this.subject == null ? other.subject == null : this.subject.equals(other.subject) &&
                    this.body == null ? other.body == null : this.body.equals(other.body) &&
                    this.sent == null ? other.sent == null : this.sent.equals(other.sent) &&
                    this.received.equals(other.received) &&
                    this.seen.equals(other.seen) &&
                    this.ui_seen.equals(other.ui_seen) &&
                    this.ui_hide.equals(other.ui_hide));
        }
        return false;
    }
}
