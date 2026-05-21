package com.algorithmx.q_base.data.di;

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
public final class AuthModule_ProvideAppwriteAccountFactory implements Factory<Account> {
  private final Provider<Client> clientProvider;

  private AuthModule_ProvideAppwriteAccountFactory(Provider<Client> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public Account get() {
    return provideAppwriteAccount(clientProvider.get());
  }

  public static AuthModule_ProvideAppwriteAccountFactory create(Provider<Client> clientProvider) {
    return new AuthModule_ProvideAppwriteAccountFactory(clientProvider);
  }

  public static Account provideAppwriteAccount(Client client) {
    return Preconditions.checkNotNullFromProvides(AuthModule.INSTANCE.provideAppwriteAccount(client));
  }
}
