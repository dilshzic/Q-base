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
public final class BackendModule_ProvideActiveDatabaseFactory implements Factory<CoreDatabase> {
  private final Provider<CoreDatabase> appwriteDatabaseProvider;

  private BackendModule_ProvideActiveDatabaseFactory(
      Provider<CoreDatabase> appwriteDatabaseProvider) {
    this.appwriteDatabaseProvider = appwriteDatabaseProvider;
  }

  @Override
  public CoreDatabase get() {
    return provideActiveDatabase(appwriteDatabaseProvider.get());
  }

  public static BackendModule_ProvideActiveDatabaseFactory create(
      Provider<CoreDatabase> appwriteDatabaseProvider) {
    return new BackendModule_ProvideActiveDatabaseFactory(appwriteDatabaseProvider);
  }

  public static CoreDatabase provideActiveDatabase(CoreDatabase appwriteDatabase) {
    return Preconditions.checkNotNullFromProvides(BackendModule.INSTANCE.provideActiveDatabase(appwriteDatabase));
  }
}
