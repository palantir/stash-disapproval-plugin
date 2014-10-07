// Copyright 2014 Palantir Technologies
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.palantir.stash.disapprove.persistence;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public enum DisapprovalMode {
    ADVISORY_MODE(Constants.ADVISORY_VALUE, "Advisory Mode"),
    STRICT_MODE(Constants.STRICT_VALUE, "Strict Mode (PRs cannot be merged while disaproved)");

    private final String description;
    private final String mode;

    // This is necessary because AO annotations require static string constants
    public static class Constants {

        public static final String ADVISORY_VALUE = "ADVISORY_MODE";
        public static final String STRICT_VALUE = "STRICT_MODE";
    }

    DisapprovalMode(String mode, String description) {
        this.description = description;
        this.mode = mode;
    }

    public String getDescription() {
        return description;
    }

    public String getMode() {
        return mode;
    }

    public static DisapprovalMode fromMode(String mode) {
        if (mode.equals(Constants.ADVISORY_VALUE)) {
            return ADVISORY_MODE;
        }
        if (mode.equals(Constants.STRICT_VALUE)) {
            return STRICT_MODE;
        }
        throw new IllegalArgumentException("invalid value for enum: " + mode);
    }

    public static String toMode(DisapprovalMode am) {
        return am.getMode();
    }

    /**
     * Helper method for populating a dropdown option box with metadata
     * 
     * @param selected
     * @return
     */
    public ImmutableMap<String, String> getSelectListEntry(boolean selected) {
        if (selected) {
            return ImmutableMap.of("text", this.getDescription(), "value", this.toString(), "selected", "true");
        } else {
            return ImmutableMap.of("text", this.getDescription(), "value", this.toString());
        }
    }

    /**
     * Helper method for populating a dropdown option box with metadata
     * 
     * @param selected
     * @return
     */
    public static ImmutableList<ImmutableMap<String, String>> getSelectList(DisapprovalMode selected) {
        ImmutableList.Builder<ImmutableMap<String, String>> builder = ImmutableList.builder();
        for (DisapprovalMode ae : DisapprovalMode.values()) {
            if (selected != null && selected.equals(ae)) {
                builder.add(ae.getSelectListEntry(true));
            } else {
                builder.add(ae.getSelectListEntry(false));
            }
        }
        return builder.build();
    }
}
