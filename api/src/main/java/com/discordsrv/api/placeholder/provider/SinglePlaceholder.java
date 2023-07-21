package com.discordsrv.api.placeholder.provider;

import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Supplier;

public class SinglePlaceholder implements PlaceholderProvider {

    private final String matchPlaceholder;
    private final Supplier<Object> resultProvider;

    public SinglePlaceholder(String placeholder, Object result) {
        this(placeholder, () -> result);
    }

    public SinglePlaceholder(String placeholder, Supplier<Object> resultProvider) {
        this.matchPlaceholder = placeholder;
        this.resultProvider = resultProvider;
    }

    @Override
    public @NotNull PlaceholderLookupResult lookup(@NotNull String placeholder, @NotNull Set<Object> context) {
        if (!placeholder.equals(matchPlaceholder)) {
            return PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
        }

        try {
            return PlaceholderLookupResult.success(
                    resultProvider.get()
            );
        } catch (Throwable t) {
            return PlaceholderLookupResult.lookupFailed(t);
        }
    }
}
