package com.algorithmx.q_base.data.backend;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.appwrite.Client;
import io.appwrite.services.Databases;
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
public final class AppwriteDatabaseImpl_Factory implements Factory<AppwriteDatabaseImpl> {
  private final Provider<Client> clientProvider;

  private final Provider<Databases> databasesProvider;

  private final Provider<Object> tablesClientProvider;

  private AppwriteDatabaseImpl_Factory(Provider<Client> clientProvider,
      Provider<Databases> databasesProvider, Provider<Object> tablesClientProvider) {
    this.clientProvider = clientProvider;
    this.databasesProvider = databasesProvider;
    this.tablesClientProvider = tablesClientProvider;
  }

  @Override
  public AppwriteDatabaseImpl get() {
    return newInstance(clientProvider.get(), databasesProvider.get(), tablesClientProvider.get());
  }

  public static AppwriteDatabaseImpl_Factory create(Provider<Client> clientProvider,
      Provider<Databases> databasesProvider, Provider<Object> tablesClientProvider) {
    return new AppwriteDatabaseImpl_Factory(clientProvider, databasesProvider, tablesClientProvider);
  }

  public static AppwriteDatabaseImpl newInstance(Client client, Databases databases,
      Object tablesClient) {
    return new AppwriteDatabaseImpl(client, databases, tablesClient);
  }
}
