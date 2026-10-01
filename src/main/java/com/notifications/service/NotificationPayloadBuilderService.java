package com.notifications.service;

import com.notifications.config.RedisConfig;
import com.notifications.dto.NotificationPayload;
import com.notifications.dto.NotificationRequest;
import com.notifications.model.Device;
import com.notifications.model.NotificationTemplate;
import com.notifications.model.User;
import com.notifications.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a fully-rendered NotificationPayload from a request, template, and user/device context.
 * Supports {{variableName}} placeholder substitution.
 */
@Service
public class NotificationPayloadBuilderService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPayloadBuilderService.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final NotificationTemplateRepository templateRepository;

    public NotificationPayloadBuilderService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Cacheable(value = RedisConfig.CACHE_TEMPLATES, key = "#templateName")
    public NotificationTemplate getTemplate(String templateName) {
        return templateRepository.findByName(templateName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification template not found: " + templateName));
    }

    public NotificationPayload buildPushPayload(NotificationRequest request,
                                                 User user,
                                                 Device device,
                                                 Long logId) {
        NotificationTemplate template = getTemplate(request.getTemplateName());
        Map<String, String> vars = buildVariables(request.getVariables(), user);

        return NotificationPayload.builder()
                .logId(logId)
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .userPhone(user.getPhone())
                .deviceToken(device.getToken())
                .devicePlatform(device.getPlatform())
                .channel(request.getChannel())
                .templateName(request.getTemplateName())
                .title(render(template.getTitleTemplate(), vars))
                .body(render(template.getBodyTemplate(), vars))
                .retryCount(0)
                .build();
    }

    public NotificationPayload buildChannelPayload(NotificationRequest request,
                                                    User user,
                                                    Long logId) {
        NotificationTemplate template = getTemplate(request.getTemplateName());
        Map<String, String> vars = buildVariables(request.getVariables(), user);

        return NotificationPayload.builder()
                .logId(logId)
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .userPhone(user.getPhone())
                .channel(request.getChannel())
                .templateName(request.getTemplateName())
                .title(render(template.getTitleTemplate(), vars))
                .body(render(template.getBodyTemplate(), vars))
                .retryCount(0)
                .build();
    }

    private Map<String, String> buildVariables(Map<String, String> callerVars, User user) {
        Map<String, String> vars = new HashMap<>(callerVars);
        vars.putIfAbsent("userName", user.getName());
        vars.putIfAbsent("userEmail", user.getEmail());
        vars.putIfAbsent("userPhone", user.getPhone() != null ? user.getPhone() : "");
        return vars;
    }

    public String render(String template, Map<String, String> vars) {
        if (template == null) return null;
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = vars.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
