package com.algorithmx.q_base.data.chat;

import com.algorithmx.q_base.data.backend.CoreDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class ChatRemoteRepository_Factory implements Factory<ChatRemoteRepository> {
  private final Provider<CoreDatabase> databasesProvider;

  private final Provider<Account> appwriteAccountProvider;

  private ChatRemoteRepository_Factory(Provider<CoreDatabase> databasesProvider,
      Provider<Account> appwriteAccountProvider) {
    this.databasesProvider = databasesProvider;
    this.appwriteAccountProvider = appwriteAccountProvider;
  }

  @Override
  public ChatRemoteRepository get() {
    return newInstance(databasesProvider.get(), appwriteAccountProvider.get());
  }

  public static ChatRemoteRepository_Factory create(Provider<CoreDatabase> databasesProvider,
      Provider<Account> appwriteAccountProvider) {
    return new ChatRemoteRepository_Factory(databasesProvider, appwriteAccountProvider);
  }

  public static ChatRemoteRepository newInstance(CoreDatabase databases, Account appwriteAccount) {
    return new ChatRemoteRepository(databases, appwriteAccount);
  }
}
