package com.schemaplexai.context.service;

import java.util.List;

public interface RagService {

    List<String> retrieve(String query, int topK);
}
