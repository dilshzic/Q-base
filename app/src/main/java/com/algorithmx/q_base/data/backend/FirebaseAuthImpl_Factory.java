package com.algorithmx.q_base.data.backend;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
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
public final class FirebaseAuthImpl_Factory implements Factory<FirebaseAuthImpl> {
  private final Provider<FirebaseAuth> firebaseAuthProvider;

  private final Provider<Context> contextProvider;

  private FirebaseAuthImpl_Factory(Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<Context> contextProvider) {
    this.firebaseAuthProvider = firebaseAuthProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public FirebaseAuthImpl get() {
    return newInstance(firebaseAuthProvider.get(), contextProvider.get());
  }

  public static FirebaseAuthImpl_Factory create(Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<Context> contextProvider) {
    return new FirebaseAuthImpl_Factory(firebaseAuthProvider, contextProvider);
  }

  public static FirebaseAuthImpl newInstance(FirebaseAuth firebaseAuth, Context context) {
    return new FirebaseAuthImpl(firebaseAuth, context);
  }
}
