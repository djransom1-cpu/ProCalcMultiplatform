// Cloud sync bridge for the web build. Mirrors CloudSyncManager / AuthManager in the Android app
// (ConstructionCalculatorAndroid) exactly - same Firebase project, same collections, same fields
// and the same order of writes - so projects move between the phone and the web and pass the same
// Firestore security rules. Every function returns a Promise of a JSON string for the Kotlin side.
(function () {
    var FIREBASE_CONFIG = {
        apiKey: "AIzaSyDdT5wY9efnRWgngFKDx-PdimVC3v9_JZs",
        authDomain: "pro-construction-calculator.firebaseapp.com",
        projectId: "pro-construction-calculator",
        storageBucket: "pro-construction-calculator.firebasestorage.app",
        messagingSenderId: "264889716945",
        appId: "1:264889716945:web:4cf35c5d367ff949b835f0"
    };

    var PROJECTS = "projects";
    var INVITE_CODES = "inviteCodes";
    var INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I

    var ready = null;

    function configured() {
        return FIREBASE_CONFIG.apiKey.indexOf("REPLACE_") !== 0 && FIREBASE_CONFIG.appId.indexOf("REPLACE_") !== 0;
    }

    function init() {
        if (ready) return ready;
        ready = new Promise(function (resolve, reject) {
            if (!configured()) { reject(new Error("Cloud sync isn't configured yet")); return; }
            if (typeof firebase === "undefined") { reject(new Error("Couldn't load Firebase. Check your connection.")); return; }
            if (!firebase.apps.length) firebase.initializeApp(FIREBASE_CONFIG);
            var unsubscribe = firebase.auth().onAuthStateChanged(function () {
                unsubscribe();
                resolve();
            }, reject);
        });
        ready.catch(function () { ready = null; });
        return ready;
    }

    function db() { return firebase.firestore(); }
    function user() { return firebase.auth().currentUser; }
    function requireUser() {
        var u = user();
        if (!u) throw new Error("Not signed in");
        return u;
    }
    function userJson(u) { return u ? { uid: u.uid, email: u.email || "" } : null; }
    function millis(ts) { return ts && ts.toMillis ? ts.toMillis() : 0; }
    function generateInviteCode() {
        var code = "";
        for (var i = 0; i < 6; i++) code += INVITE_CODE_CHARS.charAt(Math.floor(Math.random() * INVITE_CODE_CHARS.length));
        return code;
    }
    function friendlyError(e) {
        var code = (e && e.code) || "";
        if (code === "auth/popup-closed-by-user" || code === "auth/cancelled-popup-request") return "Sign-in was cancelled";
        if (code === "auth/popup-blocked") return "Your browser blocked the sign-in popup. Allow popups for this site and try again.";
        if (code === "auth/unauthorized-domain") return "This website isn't authorized for sign-in yet (add it under Firebase Authentication > Authorized domains)";
        if (code === "permission-denied") return "The cloud refused access to this project";
        if (code === "unavailable") return "Can't reach the cloud. Check your connection.";
        return (e && e.message) || String(e);
    }

    // Always resolves: {"ok": result} or {"error": "message"} - Kotlin/Wasm can't read JS error details.
    function run(fn) {
        return init().then(fn).then(
            function (result) { return JSON.stringify({ ok: result === undefined ? null : result }); },
            function (e) { return JSON.stringify({ error: friendlyError(e) }); }
        );
    }

    window.ProCalcCloud = {
        currentUser: function () { return run(function () { return userJson(user()); }); },

        signIn: function () {
            return run(function () {
                var provider = new firebase.auth.GoogleAuthProvider();
                return firebase.auth().signInWithPopup(provider).then(function (cred) { return userJson(cred.user); });
            });
        },

        signOut: function () { return run(function () { return firebase.auth().signOut(); }); },

        listProjects: function () {
            return run(function () {
                var u = requireUser();
                return db().collection(PROJECTS).where("memberUids", "array-contains", u.uid).get().then(function (snap) {
                    return snap.docs.map(function (doc) {
                        var d = doc.data();
                        return {
                            cloudId: doc.id,
                            displayName: d.displayName || "Unnamed Project",
                            ownerEmail: d.ownerEmail || "",
                            memberCount: (d.memberUids || []).length || 1,
                            updatedAtMillis: millis(d.updatedAt)
                        };
                    });
                });
            });
        },

        getProject: function (cloudId) {
            return run(function () {
                return db().collection(PROJECTS).doc(cloudId).get().then(function (doc) {
                    if (!doc.exists) throw new Error("Cloud project not found");
                    var d = doc.data();
                    if (typeof d.dataJson !== "string") throw new Error("Cloud project has no data");
                    return {
                        cloudId: doc.id,
                        displayName: d.displayName || "Cloud Project",
                        dataJson: d.dataJson,
                        updatedAtMillis: millis(d.updatedAt) || Date.now()
                    };
                });
            });
        },

        // Same as CloudSyncManager.enableCloudSync: project doc first, then its invite code
        // (sequential - the inviteCodes rule reads the project doc).
        createProject: function (argsJson) {
            return run(function () {
                var u = requireUser();
                var args = JSON.parse(argsJson);
                var cloudId = crypto.randomUUID();
                var inviteCode = generateInviteCode();
                var memberEmails = {};
                memberEmails[u.uid] = u.email || "";
                var projectRef = db().collection(PROJECTS).doc(cloudId);
                return projectRef.set({
                    ownerUid: u.uid,
                    ownerEmail: u.email || "",
                    displayName: args.displayName,
                    memberUids: [u.uid],
                    memberEmails: memberEmails,
                    inviteCode: inviteCode,
                    dataJson: args.dataJson,
                    blueprintFiles: [],
                    updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
                    updatedByUid: u.uid
                }).then(function () {
                    return db().collection(INVITE_CODES).doc(inviteCode).set({ projectId: cloudId });
                }).then(function () {
                    return { cloudId: cloudId, syncedAtMillis: Date.now() };
                });
            });
        },

        // Same as CloudSyncManager.pushProject.
        pushProject: function (argsJson) {
            return run(function () {
                var u = requireUser();
                var args = JSON.parse(argsJson);
                return db().collection(PROJECTS).doc(args.cloudId).update({
                    dataJson: args.dataJson,
                    displayName: args.displayName,
                    updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
                    updatedByUid: u.uid
                });
            });
        },

        // Same as CloudSyncManager.joinProjectByCode (two updates, matching the join rule).
        joinByCode: function (code) {
            return run(function () {
                var u = requireUser();
                var normalized = code.trim().toUpperCase();
                return db().collection(INVITE_CODES).doc(normalized).get().then(function (codeDoc) {
                    var projectId = codeDoc.exists ? codeDoc.data().projectId : null;
                    if (!projectId) throw new Error("Invalid invite code");
                    var projectRef = db().collection(PROJECTS).doc(projectId);
                    return projectRef.update({ memberUids: firebase.firestore.FieldValue.arrayUnion(u.uid) })
                        .then(function () {
                            var update = {};
                            update["memberEmails." + u.uid] = u.email || "";
                            return projectRef.update(update);
                        })
                        .then(function () { return { cloudId: projectId }; });
                });
            });
        },

        teamInfo: function (cloudId) {
            return run(function () {
                var u = requireUser();
                return db().collection(PROJECTS).doc(cloudId).get().then(function (doc) {
                    var d = doc.data() || {};
                    var emails = d.memberEmails || {};
                    return {
                        inviteCode: d.inviteCode || "",
                        ownerUid: d.ownerUid || "",
                        ownerEmail: d.ownerEmail || "",
                        memberEmails: Object.keys(emails).map(function (k) { return emails[k]; }),
                        isOwner: d.ownerUid === u.uid
                    };
                });
            });
        },

        // Same as CloudSyncManager.regenerateInviteCode (owner only).
        regenerateCode: function (cloudId) {
            return run(function () {
                var u = requireUser();
                var projectRef = db().collection(PROJECTS).doc(cloudId);
                return projectRef.get().then(function (doc) {
                    var d = doc.data() || {};
                    if (d.ownerUid !== u.uid) throw new Error("Only the project owner can regenerate the invite code");
                    var newCode = generateInviteCode();
                    var batch = db().batch();
                    batch.set(db().collection(INVITE_CODES).doc(newCode), { projectId: cloudId });
                    batch.update(projectRef, { inviteCode: newCode });
                    if (d.inviteCode) batch.delete(db().collection(INVITE_CODES).doc(d.inviteCode));
                    return batch.commit().then(function () { return { inviteCode: newCode }; });
                });
            });
        },

        // Same as CloudSyncManager.deleteCloudProject (owner) / leaveCloudProject (member).
        removeProject: function (cloudId) {
            return run(function () {
                var u = requireUser();
                var projectRef = db().collection(PROJECTS).doc(cloudId);
                return projectRef.get().then(function (doc) {
                    if (!doc.exists) return { removed: "none" };
                    var d = doc.data();
                    if (d.ownerUid !== u.uid) {
                        var leave = { memberUids: firebase.firestore.FieldValue.arrayRemove(u.uid) };
                        leave["memberEmails." + u.uid] = firebase.firestore.FieldValue.delete();
                        return projectRef.update(leave).then(function () { return { removed: "left" }; });
                    }
                    var files = d.blueprintFiles || [];
                    var deletes = files.map(function (name) {
                        return firebase.storage().ref().child("projects/" + cloudId + "/blueprints/" + name)
                            .delete().catch(function () { /* best effort, like the app */ });
                    });
                    return Promise.all(deletes).then(function () {
                        var batch = db().batch();
                        if (d.inviteCode) batch.delete(db().collection(INVITE_CODES).doc(d.inviteCode));
                        batch.delete(projectRef);
                        return batch.commit();
                    }).then(function () { return { removed: "deleted" }; });
                });
            });
        }
    };
})();
