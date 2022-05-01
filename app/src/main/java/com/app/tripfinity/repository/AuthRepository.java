package com.app.tripfinity.repository;

import static com.app.tripfinity.utils.HelperClass.enableFCM;
import static com.app.tripfinity.utils.HelperClass.logErrorMessage;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;

import com.app.tripfinity.model.User;
import com.app.tripfinity.utils.Constants;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AuthRepository {
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore rootRef = FirebaseFirestore.getInstance();
    private CollectionReference usersRef = rootRef.collection(Constants.USER_COLLECTION);

    public MutableLiveData<User> firebaseSignInWithGoogle(AuthCredential googleAuthCredential) {
        MutableLiveData<User> authenticatedUserMutableLiveData = new MutableLiveData<>();
        firebaseAuth.signInWithCredential(googleAuthCredential).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                    String uid = firebaseUser.getUid();
                    String name = firebaseUser.getDisplayName();
                    String email = firebaseUser.getEmail();
                    Uri userPhotoUrl = firebaseUser.getPhotoUrl();
                    User user = new User(uid, name, email);
                    if(userPhotoUrl != null) {
                        user.setUserPhotoUrl(userPhotoUrl.toString());
                    }
                    authenticatedUserMutableLiveData.setValue(user);
                    enableFCM();
                }
            } else {
                logErrorMessage(Objects.requireNonNull(authTask.getException()).getMessage());
            }
        });
        return authenticatedUserMutableLiveData;
    }

    public MutableLiveData<User> createUserInFirestoreIfNotExists(User authenticatedUser, boolean isRegistered, String tripId) {
        MutableLiveData<User> newUserMutableLiveData = new MutableLiveData<>();
        if(tripId != null){
            DocumentReference tripRef = rootRef.collection("Trips").document(tripId);
            List<DocumentReference> tripList = new ArrayList<>();
            tripList.add(tripRef);
            authenticatedUser.setTrips(tripList);
        }
        DocumentReference uidRef = usersRef.document(authenticatedUser.getEmail());
        uidRef.get().addOnCompleteListener(uidTask -> {
            if (uidTask.isSuccessful()) {
                DocumentSnapshot document = uidTask.getResult();
                authenticatedUser.setIsRegistered(isRegistered);
                if (!document.exists()) {
                    uidRef.set(authenticatedUser).addOnCompleteListener(userCreationTask -> {
                        if (userCreationTask.isSuccessful()) {
                            newUserMutableLiveData.setValue(authenticatedUser);
                        } else {
                            logErrorMessage(Objects.requireNonNull(userCreationTask.getException()).getMessage());
                        }
                    });
                } else {
                    newUserMutableLiveData.setValue(authenticatedUser);
                }
            } else {
                logErrorMessage(Objects.requireNonNull(uidTask.getException()).getMessage());
            }
        });
        return newUserMutableLiveData;
    }
}