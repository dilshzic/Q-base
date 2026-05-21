package com.algorithmx.q_base.data.backend;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "com.algorithmx.q_base.data.backend.FirebaseBackend",
    "dagger.hilt.android.qualifiers.ApplicationContext"
})
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
public final class BackendModule_ProvideFirebaseAuthImplFactory implements Factory<CoreAuth> {
  private final Provider<FirebaseAuth> firebaseAuthProvider;

  private final Provider<Context> contextProvider;

  private BackendModule_ProvideFirebaseAuthImplFactory(Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<Context> contextProvider) {
    this.firebaseAuthProvider = firebaseAuthProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public CoreAuth get() {
    return provideFirebaseAuthImpl(firebaseAuthProvider.get(), contextProvider.get());
  }

  public static BackendModule_ProvideFirebaseAuthImplFactory create(
      Provider<FirebaseAuth> firebaseAuthProvider, Provider<Context> contextProvider) {
    return new BackendModule_ProvideFirebaseAuthImplFactory(firebaseAuthProvider, contextProvider);
  }

  public static CoreAuth provideFirebaseAuthImpl(FirebaseAuth firebaseAuth, Context context) {
    return Preconditions.checkNotNullFromProvides(BackendModule.INSTANCE.provideFirebaseAuthImpl(firebaseAuth, context));
  }
}
