package com.algorithmx.q_base.data.auth;

import com.algorithmx.q_base.core_crypto.CryptoManager;
import com.algorithmx.q_base.data.backend.CoreAuth;
import com.algorithmx.q_base.data.backend.CoreDatabase;
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
public final class ProfileRepository_Factory implements Factory<ProfileRepository> {
  private final Provider<CoreDatabase> coreDatabaseProvider;

  private final Provider<CoreAuth> coreAuthProvider;

  private final Provider<ProfileCache> profileCacheProvider;

  private final Provider<CryptoManager> cryptoManagerProvider;

  private ProfileRepository_Factory(Provider<CoreDatabase> coreDatabaseProvider,
      Provider<CoreAuth> coreAuthProvider, Provider<ProfileCache> profileCacheProvider,
      Provider<CryptoManager> cryptoManagerProvider) {
    this.coreDatabaseProvider = coreDatabaseProvider;
    this.coreAuthProvider = coreAuthProvider;
    this.profileCacheProvider = profileCacheProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
  }

  @Override
  public ProfileRepository get() {
    return newInstance(coreDatabaseProvider.get(), coreAuthProvider.get(), profileCacheProvider.get(), cryptoManagerProvider.get());
  }

  public static ProfileRepository_Factory create(Provider<CoreDatabase> coreDatabaseProvider,
      Provider<CoreAuth> coreAuthProvider, Provider<ProfileCache> profileCacheProvider,
      Provider<CryptoManager> cryptoManagerProvider) {
    return new ProfileRepository_Factory(coreDatabaseProvider, coreAuthProvider, profileCacheProvider, cryptoManagerProvider);
  }

  public static ProfileRepository newInstance(CoreDatabase coreDatabase, CoreAuth coreAuth,
      ProfileCache profileCache, CryptoManager cryptoManager) {
    return new ProfileRepository(coreDatabase, coreAuth, profileCache, cryptoManager);
  }
}
