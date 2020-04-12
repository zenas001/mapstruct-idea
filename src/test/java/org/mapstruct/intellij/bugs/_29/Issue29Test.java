/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.intellij.bugs._29;

import com.intellij.codeInsight.lookup.LookupElement;
import org.mapstruct.intellij.MapstructBaseCompletionTestCase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Filip Hrisafov
 */
public class Issue29Test extends MapstructBaseCompletionTestCase {

    @Override
    protected String getTestDataPath() {
        return "testData/bugs/_29";
    }

    public void testDoesNotAutoComplete() {
        configureByFile( "/Issue29Data.java" );
        assertThat( myItems )
            .extracting( LookupElement::getLookupString )
            .containsExactlyInAnyOrder(
                "propA1",
                "propA2"
            );
    }
}
