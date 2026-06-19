package com.dbsyncstudio.core.mapping;

import com.dbsyncstudio.model.metadata.ColumnMetadata;
import com.dbsyncstudio.model.sync.FieldMappingSuggestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FieldMappingSuggestionMatcher {

    private static final Map<String, String> COMMON_ALIASES = buildCommonAliases();
    private static final Set<String> STOP_WORDS = buildStopWords();

    public List<FieldMappingSuggestion> match(List<ColumnMetadata> sourceColumns, List<ColumnMetadata> targetColumns) {
        List<FieldMappingSuggestion> suggestions = new ArrayList<FieldMappingSuggestion>();
        if (sourceColumns == null || sourceColumns.isEmpty()) {
            return suggestions;
        }

        List<ColumnMetadata> safeTargets = targetColumns == null ? new ArrayList<ColumnMetadata>() : targetColumns;
        Set<String> usedTargetNames = new LinkedHashSet<String>();

        for (ColumnMetadata sourceColumn : sourceColumns) {
            if (sourceColumn == null || sourceColumn.getName() == null || sourceColumn.getName().trim().length() == 0) {
                continue;
            }
            MatchCandidate bestMatch = findBestMatch(sourceColumn.getName(), safeTargets, usedTargetNames);
            FieldMappingSuggestion suggestion = new FieldMappingSuggestion();
            suggestion.setSourceColumnName(sourceColumn.getName());
            suggestion.setTargetColumnName(bestMatch.targetColumnName);
            suggestion.setConfidence(bestMatch.confidence);
            suggestion.setMatchReason(bestMatch.matchReason);
            suggestion.setIgnored(bestMatch.targetColumnName == null);
            suggestions.add(suggestion);
            if (bestMatch.targetColumnName != null) {
                usedTargetNames.add(normalize(bestMatch.targetColumnName));
            }
        }
        return suggestions;
    }

    private MatchCandidate findBestMatch(String sourceColumnName, List<ColumnMetadata> targetColumns, Set<String> usedTargetNames) {
        String normalizedSource = normalize(sourceColumnName);
        MatchCandidate bestMatch = MatchCandidate.none();

        for (ColumnMetadata targetColumn : targetColumns) {
            if (targetColumn == null || targetColumn.getName() == null || targetColumn.getName().trim().length() == 0) {
                continue;
            }
            if (usedTargetNames.contains(normalize(targetColumn.getName()))) {
                continue;
            }
            MatchCandidate candidate = scoreMatch(sourceColumnName, targetColumn.getName(), normalizedSource, normalize(targetColumn.getName()));
            if (candidate.confidence > bestMatch.confidence) {
                bestMatch = candidate;
            }
        }

        if (bestMatch.confidence < 0.45d) {
            return MatchCandidate.none();
        }
        return bestMatch;
    }

    private MatchCandidate scoreMatch(String sourceColumnName, String targetColumnName, String normalizedSource, String normalizedTarget) {
        if (normalizedSource.length() == 0 || normalizedTarget.length() == 0) {
            return MatchCandidate.none();
        }

        if (sourceColumnName.equals(targetColumnName)) {
            return new MatchCandidate(targetColumnName, 1.0d, "exact");
        }

        if (sourceColumnName.equalsIgnoreCase(targetColumnName)) {
            return new MatchCandidate(targetColumnName, 0.98d, "ignore_case");
        }

        if (normalizedSource.equals(normalizedTarget)) {
            return new MatchCandidate(targetColumnName, 0.95d, "normalized");
        }

        if (aliasMatch(normalizedSource, normalizedTarget)) {
            return new MatchCandidate(targetColumnName, aliasConfidence(normalizedSource, normalizedTarget), "synonym");
        }

        double tokenScore = tokenSimilarity(normalizedSource, normalizedTarget);
        double editScore = normalizedEditSimilarity(normalizedSource, normalizedTarget);
        double prefixScore = prefixSimilarity(normalizedSource, normalizedTarget);
        double confidence = Math.max(tokenScore, Math.max(editScore, prefixScore));
        double idScore = weakIdentifierMatch(normalizedSource, normalizedTarget);
        confidence = Math.max(confidence, idScore);
        if (confidence >= 0.45d) {
            String reason = confidence >= 0.72d ? "fuzzy" : "fuzzy";
            if (idScore >= confidence && idScore > 0.0d) {
                reason = "fuzzy";
            }
            return new MatchCandidate(targetColumnName, roundConfidence(confidence), reason);
        }
        return MatchCandidate.none();
    }

    private boolean aliasMatch(String normalizedSource, String normalizedTarget) {
        String sourceAlias = COMMON_ALIASES.get(normalizedSource);
        if (sourceAlias != null && sourceAlias.equals(normalizedTarget)) {
            return true;
        }
        String targetAlias = COMMON_ALIASES.get(normalizedTarget);
        return targetAlias != null && targetAlias.equals(normalizedSource);
    }

    private double aliasConfidence(String normalizedSource, String normalizedTarget) {
        if ("id".equals(normalizedSource) || "id".equals(normalizedTarget)) {
            return 0.58d;
        }
        return 0.92d;
    }

    private double weakIdentifierMatch(String normalizedSource, String normalizedTarget) {
        if ("id".equals(normalizedSource) && normalizedTarget.endsWith("id") && normalizedTarget.length() > 2) {
            return 0.56d;
        }
        if ("id".equals(normalizedTarget) && normalizedSource.endsWith("id") && normalizedSource.length() > 2) {
            return 0.56d;
        }
        return 0.0d;
    }

    private double tokenSimilarity(String normalizedSource, String normalizedTarget) {
        Set<String> sourceTokens = tokenize(normalizedSource);
        Set<String> targetTokens = tokenize(normalizedTarget);
        if (sourceTokens.isEmpty() || targetTokens.isEmpty()) {
            return 0.0d;
        }

        int overlap = 0;
        for (String token : sourceTokens) {
            if (targetTokens.contains(token)) {
                overlap++;
            }
        }
        int maxSize = Math.max(sourceTokens.size(), targetTokens.size());
        return maxSize == 0 ? 0.0d : ((double) overlap) / ((double) maxSize);
    }

    private double normalizedEditSimilarity(String normalizedSource, String normalizedTarget) {
        int maxLength = Math.max(normalizedSource.length(), normalizedTarget.length());
        if (maxLength == 0) {
            return 0.0d;
        }
        int distance = levenshteinDistance(normalizedSource, normalizedTarget);
        return 1.0d - (((double) distance) / ((double) maxLength));
    }

    private double prefixSimilarity(String normalizedSource, String normalizedTarget) {
        String shorter = normalizedSource.length() <= normalizedTarget.length() ? normalizedSource : normalizedTarget;
        String longer = normalizedSource.length() > normalizedTarget.length() ? normalizedSource : normalizedTarget;
        if (shorter.length() < 3) {
            return 0.0d;
        }
        if (longer.startsWith(shorter)) {
            double ratio = ((double) shorter.length()) / ((double) longer.length());
            return Math.max(0.7d, ratio);
        }
        return 0.0d;
    }

    private int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int substitution = previous[j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                int insertion = current[j - 1] + 1;
                int deletion = previous[j] + 1;
                current[j] = Math.min(substitution, Math.min(insertion, deletion));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private Set<String> tokenize(String value) {
        Set<String> tokens = new LinkedHashSet<String>();
        if (value == null) {
            return tokens;
        }
        String[] parts = value.split("_");
        for (String part : parts) {
            String token = normalizeToken(part);
            if (token.length() > 0 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lowerCase = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lowerCase.length(); i++) {
            char ch = lowerCase.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(ch);
            } else if (ch == '_') {
                builder.append('_');
            }
        }
        String normalized = builder.toString();
        String[] parts = normalized.split("_");
        StringBuilder collapsed = new StringBuilder();
        for (String part : parts) {
            String token = normalizeToken(part);
            if (token.length() == 0 || STOP_WORDS.contains(token)) {
                continue;
            }
            collapsed.append(token);
        }
        return collapsed.length() == 0 ? normalized.replace("_", "") : collapsed.toString();
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private double roundConfidence(double confidence) {
        return Math.round(confidence * 100.0d) / 100.0d;
    }

    private static Map<String, String> buildCommonAliases() {
        Map<String, String> aliases = new LinkedHashMap<String, String>();
        aliases.put("user", "username");
        aliases.put("username", "username");
        aliases.put("user_name", "username");
        aliases.put("phone", "mobile");
        aliases.put("mobile", "mobile");
        aliases.put("tel", "phone");
        aliases.put("create", "gmtcreate");
        aliases.put("created", "gmtcreate");
        aliases.put("gmtcreate", "gmtcreate");
        aliases.put("create_time", "gmtcreate");
        aliases.put("modified", "gmtmodified");
        aliases.put("update", "gmtmodified");
        aliases.put("updated", "gmtmodified");
        aliases.put("gmtmodified", "gmtmodified");
        aliases.put("update_time", "gmtmodified");
        return aliases;
    }

    private static Set<String> buildStopWords() {
        return new LinkedHashSet<String>(Arrays.asList("id", "no", "num", "code", "name", "time", "date"));
    }

    private static final class MatchCandidate {
        private final String targetColumnName;
        private final double confidence;
        private final String matchReason;

        private MatchCandidate(String targetColumnName, double confidence, String matchReason) {
            this.targetColumnName = targetColumnName;
            this.confidence = confidence;
            this.matchReason = matchReason;
        }

        private static MatchCandidate none() {
            return new MatchCandidate(null, 0.0d, "no_match");
        }
    }
}
