package com.algorithmx.q_base.core.data.backend;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.appwrite.Client;
import io.appwrite.services.Account;
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
public final class BackendModule_ProvideAppwriteAuthImplFactory implements Factory<CoreAuth> {
  private final Provider<Client> appwriteClientProvider;

  private final Provider<Account> appwriteAccountProvider;

  private BackendModule_ProvideAppwriteAuthImplFactory(Provider<Client> appwriteClientProvider,
      Provider<Account> appwriteAccountProvider) {
    this.appwriteClientProvider = appwriteClientProvider;
    this.appwriteAccountProvider = appwriteAccountProvider;
  }

  @Override
  public CoreAuth get() {
    return provideAppwriteAuthImpl(appwriteClientProvider.get(), appwriteAccountProvider.get());
  }

  public static BackendModule_ProvideAppwriteAuthImplFactory create(
      Provider<Client> appwriteClientProvider, Provider<Account> appwriteAccountProvider) {
    return new BackendModule_ProvideAppwriteAuthImplFactory(appwriteClientProvider, appwriteAccountProvider);
  }

  public static CoreAuth provideAppwriteAuthImpl(Client appwriteClient, Account appwriteAccount) {
    return Preconditions.checkNotNullFromProvides(BackendModule.INSTANCE.provideAppwriteAuthImpl(appwriteClient, appwriteAccount));
  }
}
