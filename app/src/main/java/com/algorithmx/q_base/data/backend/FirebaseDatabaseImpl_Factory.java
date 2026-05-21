package com.algorithmx.q_base.data.backend;

import com.google.firebase.firestore.FirebaseFirestore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class FirebaseDatabaseImpl_Factory implements Factory<FirebaseDatabaseImpl> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private FirebaseDatabaseImpl_Factory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public FirebaseDatabaseImpl get() {
    return newInstance(firestoreProvider.get());
  }

  public static FirebaseDatabaseImpl_Factory create(Provider<FirebaseFirestore> firestoreProvider) {
    return new FirebaseDatabaseImpl_Factory(firestoreProvider);
  }

  public static FirebaseDatabaseImpl newInstance(FirebaseFirestore firestore) {
    return new FirebaseDatabaseImpl(firestore);
  }
}
