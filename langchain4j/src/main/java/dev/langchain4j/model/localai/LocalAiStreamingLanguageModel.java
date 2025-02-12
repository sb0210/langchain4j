package dev.langchain4j.model.localai;

import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.completion.CompletionRequest;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.language.StreamingLanguageModel;
import lombok.Builder;

import java.time.Duration;

import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static java.time.Duration.ofSeconds;

public class LocalAiStreamingLanguageModel implements StreamingLanguageModel {

    private final OpenAiClient client;
    private final String modelName;
    private final Double temperature;
    private final Double topP;
    private final Integer maxTokens;

    @Builder
    public LocalAiStreamingLanguageModel(String baseUrl,
                                         String modelName,
                                         Double temperature,
                                         Double topP,
                                         Integer maxTokens,
                                         Duration timeout,
                                         Boolean logRequests,
                                         Boolean logResponses) {

        temperature = temperature == null ? 0.7 : temperature;
        timeout = timeout == null ? ofSeconds(60) : timeout;

        this.client = OpenAiClient.builder()
                .openAiApiKey("ignored")
                .baseUrl(ensureNotBlank(baseUrl, "baseUrl"))
                .callTimeout(timeout)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .logRequests(logRequests)
                .logStreamingResponses(logResponses)
                .build();
        this.modelName = ensureNotBlank(modelName, "modelName");
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
    }

    @Override
    public void process(String text, StreamingResponseHandler handler) {

        CompletionRequest request = CompletionRequest.builder()
                .model(modelName)
                .prompt(text)
                .temperature(temperature)
                .topP(topP)
                .maxTokens(maxTokens)
                .build();

        client.completion(request)
                .onPartialResponse(partialResponse -> {
                    String partialResponseText = partialResponse.text();
                    if (partialResponseText != null) {
                        handler.onNext(partialResponseText);
                    }
                })
                .onComplete(handler::onComplete)
                .onError(handler::onError)
                .execute();
    }
}
