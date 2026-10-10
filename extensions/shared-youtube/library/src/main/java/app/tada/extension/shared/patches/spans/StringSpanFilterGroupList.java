/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to TADa contributions.
 */

package app.tada.extension.shared.patches.spans;

import app.tada.extension.shared.StringTrieSearch;

public final class StringSpanFilterGroupList extends SpanFilterGroupList<CharSequence, StringSpanFilterGroup> {
    protected StringTrieSearch createSearchGraph() {
        return new StringTrieSearch();
    }
}
