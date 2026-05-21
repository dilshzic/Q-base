package com.algorithmx.q_base.data.backend;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.appwrite.Client;
import io.appwrite.services.Account;
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
public final class AppwriteAuthImpl_Factory implements Factory<AppwriteAuthImpl> {
  private final Provider<Client> appwriteClientProvider;

  private final Provider<Account> appwriteAccountProvider;

  private AppwriteAuthImpl_Factory(Provider<Client> appwriteClientProvider,
      Provider<Account> appwriteAccountProvider) {
    this.appwriteClientProvider = appwriteClientProvider;
    this.appwriteAccountProvider = appwriteAccountProvider;
  }

  @Override
  public AppwriteAuthImpl get() {
    return newInstance(appwriteClientProvider.get(), appwriteAccountProvider.get());
  }

  public static AppwriteAuthImpl_Factory create(Provider<Client> appwriteClientProvider,
      Provider<Account> appwriteAccountProvider) {
    return new AppwriteAuthImpl_Factory(appwriteClientProvider, appwriteAccountProvider);
  }

  public static AppwriteAuthImpl newInstance(Client appwriteClient, Account appwriteAccount) {
    return new AppwriteAuthImpl(appwriteClient, appwriteAccount);
  }
}
