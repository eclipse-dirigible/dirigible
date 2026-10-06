package org.eclipse.dirigible.flowable.bpmn.docs.ai;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

public class GeminiBpmnDocGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiBpmnDocGenerator.class);

    private static final String SYSTEM_PROMPT = "You are an expert BPMN Documentation Architect.";

    private static final String BPMN_DOC_INSTRUCTIONS = """
            Analyze the attached BPMN XML file and the corresponding process diagram image.
            Generate a detailed technical documentation in Markdown (.md) format.

            CRITICAL: At the very beginning of the document, add a section for the process digram which should be embedded \s
                using this exact syntax:
                ![Process Diagram](data:image/jpeg;base64,${DIAGRAM_BASE64})

            RULES:
            1. Describe the overall process flow.
            2. For USER TASKS: Include ID, Name, and Candidate Groups.
            3. For SERVICE TASKS: If '${JSTask}' is used, find the 'handler' file path.

            Output strictly in Markdown.
            """;

    private final String apiKey;

    public GeminiBpmnDocGenerator(String apiKey) {
        this.apiKey = apiKey;
    }

    public String generateBpmnDoc(Path bpmnFile, Path bpmnImage) throws Exception {
        try {
            // doc:
            // https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/language-models/google-ai-gemini.md
            GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder builder = GoogleAiGeminiChatModel.builder();
            GoogleAiGeminiChatModel model = builder.apiKey(apiKey)
                                                   // .modelName("gemini-2.5-flash")
                                                   .modelName("gemini-3-flash-preview")
                                                   .timeout(Duration.ofMinutes(10))
                                                   // .temperature(0.1) // Low temperature for consistent documentation
                                                   // .logRequestsAndResponses(true)
                                                   .maxRetries(2)
                                                   .build();

            // Prepare inputs
            String bpmnXml = Files.readString(bpmnFile);
            String base64Image = Base64.getEncoder()
                                       .encodeToString(Files.readAllBytes(bpmnImage));

            // Construct the conversation
            ChatRequest request = ChatRequest.builder()
                                             .messages(
                                                     // 1. SET THE ROLE (System Message)
                                                     SystemMessage.from(SYSTEM_PROMPT),

                                                     // 2. SET THE DATA (Multimodal User Message)
                                                     UserMessage.from(TextContent.from("BPMN XML:\n" + bpmnXml),
                                                             ImageContent.from(base64Image, "image/png"),
                                                             TextContent.from(BPMN_DOC_INSTRUCTIONS)))
                                             .build();

            String aiResponse = model.chat(request)
                                     .aiMessage()
                                     .text();

            return aiResponse.replace("${DIAGRAM_BASE64}", base64Image);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate AI markdown for bpmnFile: " + bpmnFile, ex);
        }
    }
}
