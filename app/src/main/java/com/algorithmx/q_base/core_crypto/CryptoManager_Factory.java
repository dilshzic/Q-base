package com.algorithmx.q_base.core_crypto;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class CryptoManager_Factory implements Factory<CryptoManager> {
  private final Provider<Context> contextProvider;

  private CryptoManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CryptoManager get() {
    return newInstance(contextProvider.get());
  }

  public static CryptoManager_Factory create(Provider<Context> contextProvider) {
    return new CryptoManager_Factory(contextProvider);
  }

  public static CryptoManager newInstance(Context context) {
    return new CryptoManager(context);
  }
}
