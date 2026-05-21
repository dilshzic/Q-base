package com.algorithmx.q_base.data.backend;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.algorithmx.q_base.data.backend.AppwriteBackend")
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
public final class BackendModule_ProvideActiveAuthFactory implements Factory<CoreAuth> {
  private final Provider<CoreAuth> appwriteAuthProvider;

  private BackendModule_ProvideActiveAuthFactory(Provider<CoreAuth> appwriteAuthProvider) {
    this.appwriteAuthProvider = appwriteAuthProvider;
  }

  @Override
  public CoreAuth get() {
    return provideActiveAuth(appwriteAuthProvider.get());
  }

  public static BackendModule_ProvideActiveAuthFactory create(
      Provider<CoreAuth> appwriteAuthProvider) {
    return new BackendModule_ProvideActiveAuthFactory(appwriteAuthProvider);
  }

  public static CoreAuth provideActiveAuth(CoreAuth appwriteAuth) {
    return Preconditions.checkNotNullFromProvides(BackendModule.INSTANCE.provideActiveAuth(appwriteAuth));
  }
}
