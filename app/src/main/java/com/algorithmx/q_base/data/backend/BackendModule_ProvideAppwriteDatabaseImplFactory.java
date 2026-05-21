package com.algorithmx.q_base.data.backend;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.appwrite.Client;
import io.appwrite.services.Databases;
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
public final class BackendModule_ProvideAppwriteDatabaseImplFactory implements Factory<CoreDatabase> {
  private final Provider<Client> clientProvider;

  private final Provider<Databases> databasesProvider;

  private final Provider<Object> tablesClientProvider;

  private BackendModule_ProvideAppwriteDatabaseImplFactory(Provider<Client> clientProvider,
      Provider<Databases> databasesProvider, Provider<Object> tablesClientProvider) {
    this.clientProvider = clientProvider;
    this.databasesProvider = databasesProvider;
    this.tablesClientProvider = tablesClientProvider;
  }

  @Override
  public CoreDatabase get() {
    return provideAppwriteDatabaseImpl(clientProvider.get(), databasesProvider.get(), tablesClientProvider.get());
  }

  public static BackendModule_ProvideAppwriteDatabaseImplFactory create(
      Provider<Client> clientProvider, Provider<Databases> databasesProvider,
      Provider<Object> tablesClientProvider) {
    return new BackendModule_ProvideAppwriteDatabaseImplFactory(clientProvider, databasesProvider, tablesClientProvider);
  }

  public static CoreDatabase provideAppwriteDatabaseImpl(Client client, Databases databases,
      Object tablesClient) {
    return Preconditions.checkNotNullFromProvides(BackendModule.INSTANCE.provideAppwriteDatabaseImpl(client, databases, tablesClient));
  }
}
