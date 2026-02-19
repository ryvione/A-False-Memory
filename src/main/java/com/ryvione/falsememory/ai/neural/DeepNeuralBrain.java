package com.ryvione.falsememory.ai.neural;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.*;

public class DeepNeuralBrain {

    private static final int INPUT_NODES = 48;
    private static final int HIDDEN_LAYER1 = 32;
    private static final int HIDDEN_LAYER2 = 24;
    private static final int HIDDEN_LAYER3 = 16;
    public static final int OUTPUT_NODES = 12;
    private static final float LEARNING_RATE = 0.05f;
    private static final float MOMENTUM = 0.9f;

    private float[][] weights1 = new float[INPUT_NODES][HIDDEN_LAYER1];
    private float[][] weights2 = new float[HIDDEN_LAYER1][HIDDEN_LAYER2];
    private float[][] weights3 = new float[HIDDEN_LAYER2][HIDDEN_LAYER3];
    private float[][] weights4 = new float[HIDDEN_LAYER3][OUTPUT_NODES];

    private float[] bias1 = new float[HIDDEN_LAYER1];
    private float[] bias2 = new float[HIDDEN_LAYER2];
    private float[] bias3 = new float[HIDDEN_LAYER3];
    private float[] bias4 = new float[OUTPUT_NODES];

    private float[][] momentum1 = new float[INPUT_NODES][HIDDEN_LAYER1];
    private float[][] momentum2 = new float[HIDDEN_LAYER1][HIDDEN_LAYER2];
    private float[][] momentum3 = new float[HIDDEN_LAYER2][HIDDEN_LAYER3];
    private float[][] momentum4 = new float[HIDDEN_LAYER3][OUTPUT_NODES];

    private float[] lastInputs = new float[INPUT_NODES];
    private float[] lastOutputs = new float[OUTPUT_NODES];
    private float[] hidden1Cache = new float[HIDDEN_LAYER1];
    private float[] hidden2Cache = new float[HIDDEN_LAYER2];
    private float[] hidden3Cache = new float[HIDDEN_LAYER3];

    private int trainingCount = 0;
    private float recentLoss = 0f;
    private Random rand = new Random();

    public DeepNeuralBrain() {
        initializeWeights();
    }

    private void initializeWeights() {
        initXavierWeights(weights1, momentum1);
        initXavierWeights(weights2, momentum2);
        initXavierWeights(weights3, momentum3);
        initXavierWeights(weights4, momentum4);
    }

    private void initXavierWeights(float[][] weights, float[][] momentum) {
        int rows = weights.length;
        int cols = weights[0].length;
        float limit = (float)Math.sqrt(6.0 / (rows + cols));
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                weights[i][j] = (rand.nextFloat() - 0.5f) * 2 * limit;
                momentum[i][j] = 0;
            }
        }
    }

    public float[] predict(float[] inputs) {
        if (inputs.length != INPUT_NODES) {
            inputs = Arrays.copyOf(inputs, INPUT_NODES);
        }

        this.lastInputs = Arrays.copyOf(inputs, INPUT_NODES);

        hidden1Cache = forward(inputs, weights1, bias1, HIDDEN_LAYER1);
        hidden2Cache = forward(hidden1Cache, weights2, bias2, HIDDEN_LAYER2);
        hidden3Cache = forward(hidden2Cache, weights3, bias3, HIDDEN_LAYER3);
        lastOutputs = forward(hidden3Cache, weights4, bias4, OUTPUT_NODES);

        return lastOutputs;
    }

    private float[] forward(float[] input, float[][] weights, float[] bias, int outputSize) {
        float[] output = new float[outputSize];
        for (int i = 0; i < outputSize; i++) {
            float sum = bias[i];
            for (int j = 0; j < input.length; j++) {
                sum += input[j] * weights[j][i];
            }
            output[i] = leakyRelu(sum);
        }
        return output;
    }

    public void train(float[] targets) {
        if (targets.length != OUTPUT_NODES) {
            targets = Arrays.copyOf(targets, OUTPUT_NODES);
        }

        float[] outputErrors = new float[OUTPUT_NODES];
        float lossSum = 0f;
        for (int i = 0; i < OUTPUT_NODES; i++) {
            float error = targets[i] - lastOutputs[i];
            lossSum += error * error;
            outputErrors[i] = error * leakyReluDerivative(lastOutputs[i]);
        }
        recentLoss = lossSum / OUTPUT_NODES;

        backpropagate(hidden3Cache, weights4, bias4, outputErrors, momentum4, 3);

        float[] hidden3Errors = new float[HIDDEN_LAYER3];
        for (int i = 0; i < HIDDEN_LAYER3; i++) {
            float error = 0;
            for (int j = 0; j < OUTPUT_NODES; j++) {
                error += outputErrors[j] * weights4[i][j];
            }
            hidden3Errors[i] = error * leakyReluDerivative(hidden3Cache[i]);
        }

        backpropagate(hidden2Cache, weights3, bias3, hidden3Errors, momentum3, 2);

        float[] hidden2Errors = new float[HIDDEN_LAYER2];
        for (int i = 0; i < HIDDEN_LAYER2; i++) {
            float error = 0;
            for (int j = 0; j < HIDDEN_LAYER3; j++) {
                error += hidden3Errors[j] * weights3[i][j];
            }
            hidden2Errors[i] = error * leakyReluDerivative(hidden2Cache[i]);
        }

        backpropagate(hidden1Cache, weights2, bias2, hidden2Errors, momentum2, 1);

        float[] hidden1Errors = new float[HIDDEN_LAYER1];
        for (int i = 0; i < HIDDEN_LAYER1; i++) {
            float error = 0;
            for (int j = 0; j < HIDDEN_LAYER2; j++) {
                error += hidden2Errors[j] * weights2[i][j];
            }
            hidden1Errors[i] = error * leakyReluDerivative(hidden1Cache[i]);
        }

        backpropagate(lastInputs, weights1, bias1, hidden1Errors, momentum1, 0);

        trainingCount++;
    }

    private void backpropagate(float[] input, float[][] weights, float[] bias, float[] errors, 
                              float[][] momentumWeights, int layer) {
        for (int i = 0; i < errors.length; i++) {
            float biasUpdate = LEARNING_RATE * errors[i];
            bias[i] += biasUpdate;

            for (int j = 0; j < input.length; j++) {
                float delta = LEARNING_RATE * errors[i] * input[j];
                float newMomentum = MOMENTUM * momentumWeights[j][i] + delta;
                momentumWeights[j][i] = newMomentum;
                weights[j][i] += newMomentum;
            }
        }
    }

    private float leakyRelu(float x) {
        return x > 0 ? x : 0.01f * x;
    }

    private float leakyReluDerivative(float x) {
        return x > 0 ? 1.0f : 0.01f;
    }

    public int getTrainingCount() {
        return trainingCount;
    }

    public float getRecentLoss() {
        return recentLoss;
    }

    public void reset() {
        trainingCount = 0;
        initializeWeights();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("trainingCount", trainingCount);

        tag.put("weights1", serializeWeights(weights1));
        tag.put("weights2", serializeWeights(weights2));
        tag.put("weights3", serializeWeights(weights3));
        tag.put("weights4", serializeWeights(weights4));

        tag.put("bias1", serializeBias(bias1));
        tag.put("bias2", serializeBias(bias2));
        tag.put("bias3", serializeBias(bias3));
        tag.put("bias4", serializeBias(bias4));

        return tag;
    }

    public void load(CompoundTag tag) {
        trainingCount = tag.getInt("trainingCount");

        deserializeWeights(tag.getCompound("weights1"), weights1);
        deserializeWeights(tag.getCompound("weights2"), weights2);
        deserializeWeights(tag.getCompound("weights3"), weights3);
        deserializeWeights(tag.getCompound("weights4"), weights4);

        deserializeBias(tag.getCompound("bias1"), bias1);
        deserializeBias(tag.getCompound("bias2"), bias2);
        deserializeBias(tag.getCompound("bias3"), bias3);
        deserializeBias(tag.getCompound("bias4"), bias4);
    }

    private ListTag serializeWeights(float[][] weights) {
        ListTag list = new ListTag();
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                CompoundTag wTag = new CompoundTag();
                wTag.putFloat("w", weights[i][j]);
                list.add(wTag);
            }
        }
        return list;
    }

    private ListTag serializeBias(float[] bias) {
        ListTag list = new ListTag();
        for (float b : bias) {
            CompoundTag bTag = new CompoundTag();
            bTag.putFloat("b", b);
            list.add(bTag);
        }
        return list;
    }

    private void deserializeWeights(CompoundTag tag, float[][] weights) {
        ListTag list = tag.getList("data", Tag.TAG_COMPOUND);
        int idx = 0;
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                if (idx < list.size()) {
                    weights[i][j] = list.getCompound(idx).getFloat("w");
                    idx++;
                }
            }
        }
    }

    private void deserializeBias(CompoundTag tag, float[] bias) {
        ListTag list = tag.getList("data", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(bias.length, list.size()); i++) {
            bias[i] = list.getCompound(i).getFloat("b");
        }
    }
}
