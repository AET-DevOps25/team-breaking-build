package com.recipefy.version.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogMessages {

    public static final String RECIPE_NOT_FOUND_PRE_MESSAGE = "Requested recipe not found.";

    public static final String BRANCH_NOT_FOUND_PRE_MESSAGE = "Requested branch not found.";

    public static final String BRANCH_NOT_UNIQUE_PRE_MESSAGE = "Requested branch name is not not unique.";

    public static final String COMMIT_NOT_FOUND_PRE_MESSAGE = "Requested commit not found.";
}
