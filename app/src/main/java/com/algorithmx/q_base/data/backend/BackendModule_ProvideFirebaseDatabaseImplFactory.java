package com.algorithmx.q_base.data.backend;

import com.google.firebase.firestore.FirebaseFirestore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.algorithmx.q_base.data.backend.FirebaseBackend")
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
public final class BackendModule_ProvideFirebaseDatabaseImplFactory implements Factory<CoreDatabase> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private BackendModule_ProvideFirebaseDatabaseImplFactory(
      Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public CoreDatabase get() {
    return provideFirebaseDatabaseImpl(firestoreProvider.get());
  }

  public static BackendModule_ProvideFirebaseDatabaseImplFactory create(
      Provider<FirebaseFirestore> firestoreProvider) {
    return new BackendModule_ProvideFirebaseDatabaseImplFactory(firestoreProvider);
  }

  public static CoreDatabase provideFirebaseDatabaseImpl(FirebaseFirestore firestore) {
    return Preconditions.checkNotNullFromProvides(BackendModule.INSTANCE.provideFirebaseDatabaseImpl(firestore));
  }
}
