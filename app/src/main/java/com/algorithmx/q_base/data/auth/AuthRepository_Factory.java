package com.algorithmx.q_base.data.auth;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.appwrite.Client;
import io.appwrite.services.Account;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<Client> appwriteClientProvider;

  private final Provider<Account> appwriteAccountProvider;

  private final Provider<Context> contextProvider;

  private AuthRepository_Factory(Provider<Client> appwriteClientProvider,
      Provider<Account> appwriteAccountProvider, Provider<Context> contextProvider) {
    this.appwriteClientProvider = appwriteClientProvider;
    this.appwriteAccountProvider = appwriteAccountProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(appwriteClientProvider.get(), appwriteAccountProvider.get(), contextProvider.get());
  }

  public static AuthRepository_Factory create(Provider<Client> appwriteClientProvider,
      Provider<Account> appwriteAccountProvider, Provider<Context> contextProvider) {
    return new AuthRepository_Factory(appwriteClientProvider, appwriteAccountProvider, contextProvider);
  }

  public static AuthRepository newInstance(Client appwriteClient, Account appwriteAccount,
      Context context) {
    return new AuthRepository(appwriteClient, appwriteAccount, context);
  }
}
