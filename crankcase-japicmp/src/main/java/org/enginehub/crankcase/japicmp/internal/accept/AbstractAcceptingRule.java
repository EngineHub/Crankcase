/*
 * SPDX-FileCopyrightText: The Gradle team
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.enginehub.crankcase.japicmp.internal.accept;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import japicmp.model.JApiCompatibility;
import me.champeau.gradle.japicmp.report.AbstractContextAwareViolationRule;
import me.champeau.gradle.japicmp.report.Violation;
import me.champeau.gradle.japicmp.report.ViolationCheckContext;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractAcceptingRule extends AbstractContextAwareViolationRule {
    protected final Violation checkAcceptance(JApiCompatibility member, List<String> changes, Violation rejection) {
        ViolationCheckContext context = getContext();
        ChangeParams changeParams = UserData.getChangeParams(context);
        Set<ApiChange> seenApiChanges = UserData.getSeenApiChanges(context);
        var change = new ApiChange(
            context.getClassName(),
            Violation.describe(member),
            changes
        );
        String reason = changeParams.changeToReason().get(change);
        if (reason != null) {
            seenApiChanges.add(change);
            return Violation.accept(
                member,
                "%s. Reason for accepting this: <b>%s</b>".formatted(rejection.getHumanExplanation(), reason)
            );
        }
        String sample = prettyPrintJson(Map.of("[REASON CHANGE IS OKAY]", List.of(change)));
        return Violation.error(
            member,
            """
            %s.
            <br>
            <p>
            In order to accept this change add the following to <code>src/changes/%s</code>:
            <pre>%s</pre>
            </p>
            """.formatted(rejection.getHumanExplanation(), changeParams.changeFileName(), sample)
        );
    }

    private static String prettyPrintJson(Object acceptanceJson) {
        var stringWriter = new StringWriter();
        try (var writer = new JsonWriter(stringWriter)) {
            writer.setIndent("    ");
            new Gson().toJson(acceptanceJson, ApiChanges.TYPE_TOKEN.getType(), writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return stringWriter.toString();
    }
}
