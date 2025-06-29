package com.SEGroup.Infrastructure.Repositories.RepositoryData;

import com.SEGroup.Service.LoggerWrapper;

import java.util.function.Supplier;

public class DbSafeExecutor {
    public static <T> T safeExecute(String operation, Supplier<T> action) {
        try {
            return action.get();
        } catch (Exception e) {
            //
            LoggerWrapper.error(operation, e);
            throw new RuntimeException("Database is currently unavailable. " +
                    "Unable to perform operation: " + operation + ". Please try again later.");
        }
    }
}