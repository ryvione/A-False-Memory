package com.ryvione.falsememory.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

public class NeuralBrain {

    private static final int INPUT_NODES = 12;
    private static final int HIDDEN_NODES = 16;
    private static final int OUTPUT_NODES = 5;
    private static final float LEARNING_RATE = 0.1f;

    private float[][] hiddenWeights = new float[INPUT_NODES][HIDDEN_NODES];
    private float[][] outputWeights = new float[HIDDEN_NODES][OUTPUT_NODES];
    private float[] hiddenBias = new float[HIDDEN_NODES];
    private float[] outputBias = new float[OUTPUT_NODES];
    
    private int trainingCount = 0;
    private float[] lastInputs = new float[INPUT_NODES];
    private float[] lastOutputs = new float[OUTPUT_NODES];
    
    private Random rand = new Random();

    public NeuralBrain() {
        initializeWeights();
    }

    private void initializeWeights() {
        for (int i = 0; i < INPUT_NODES; i++) {
            for (int j = 0; j < HIDDEN_NODES; j++) {
                hiddenWeights[i][j] = (rand.nextFloat() - 0.5f) * 0.5f;
            }
        }
        for (int i = 0; i < HIDDEN_NODES; i++) {
            for (int j = 0; j < OUTPUT_NODES; j++) {
                outputWeights[i][j] = (rand.nextFloat() - 0.5f) * 0.5f;
            }
            hiddenBias[i] = 0;
        }
        for (int i = 0; i < OUTPUT_NODES; i++) {
            outputBias[i] = 0;
        }
    }

    public float[] predict(float[] inputs) {
        if (inputs.length != INPUT_NODES) {
            throw new IllegalArgumentException("Input size must be " + INPUT_NODES);
        }

        this.lastInputs = Arrays.copyOf(inputs, INPUT_NODES);

        float[] hidden = new float[HIDDEN_NODES];
        for (int i = 0; i < HIDDEN_NODES; i++) {
            float sum = hiddenBias[i];
            for (int j = 0; j < INPUT_NODES; j++) {
                sum += inputs[j] * hiddenWeights[j][i];
            }
            hidden[i] = relu(sum);
        }

        float[] output = new float[OUTPUT_NODES];
        for (int i = 0; i < OUTPUT_NODES; i++) {
            float sum = outputBias[i];
            for (int j = 0; j < HIDDEN_NODES; j++) {
                sum += hidden[j] * outputWeights[j][i];
            }
            output[i] = sigmoid(sum);
        }

        this.lastOutputs = output;
        return output;
    }

    public void train(float[] targets) {
        if (targets.length != OUTPUT_NODES) {
            throw new IllegalArgumentException("Target size must be " + OUTPUT_NODES);
        }

        float[] hidden = new float[HIDDEN_NODES];
        for (int i = 0; i < HIDDEN_NODES; i++) {
            float sum = hiddenBias[i];
            for (int j = 0; j < INPUT_NODES; j++) {
                sum += lastInputs[j] * hiddenWeights[j][i];
            }
            hidden[i] = relu(sum);
        }

        float[] outputErrors = new float[OUTPUT_NODES];
        for (int i = 0; i < OUTPUT_NODES; i++) {
            outputErrors[i] = (targets[i] - lastOutputs[i]) * sigmoidDerivative(lastOutputs[i]);
        }

        for (int i = 0; i < OUTPUT_NODES; i++) {
            outputBias[i] += LEARNING_RATE * outputErrors[i];
            for (int j = 0; j < HIDDEN_NODES; j++) {
                outputWeights[j][i] += LEARNING_RATE * outputErrors[i] * hidden[j];
            }
        }

        float[] hiddenErrors = new float[HIDDEN_NODES];
        for (int i = 0; i < HIDDEN_NODES; i++) {
            float error = 0;
            for (int j = 0; j < OUTPUT_NODES; j++) {
                error += outputErrors[j] * outputWeights[i][j];
            }
            hiddenErrors[i] = error * reluDerivative(hidden[i]);
        }

        for (int i = 0; i < HIDDEN_NODES; i++) {
            hiddenBias[i] += LEARNING_RATE * hiddenErrors[i];
            for (int j = 0; j < INPUT_NODES; j++) {
                hiddenWeights[j][i] += LEARNING_RATE * hiddenErrors[i] * lastInputs[j];
            }
        }

        trainingCount++;
    }

    private float relu(float x) {
        return Math.max(0, x);
    }

    private float reluDerivative(float x) {
        return x > 0 ? 1 : 0;
    }

    private float sigmoid(float x) {
        return 1.0f / (1.0f + (float)Math.exp(-Math.min(Math.max(x, -500), 500)));
    }

    private float sigmoidDerivative(float x) {
        return x * (1 - x);
    }

    public int getTrainingCount() {
        return trainingCount;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("trainingCount", trainingCount);

        ListTag hiddenWeightsList = new ListTag();
        for (int i = 0; i < INPUT_NODES; i++) {
            for (int j = 0; j < HIDDEN_NODES; j++) {
                CompoundTag wTag = new CompoundTag();
                wTag.putFloat("w", hiddenWeights[i][j]);
                hiddenWeightsList.add(wTag);
            }
        }
        tag.put("hiddenWeights", hiddenWeightsList);

        ListTag outputWeightsList = new ListTag();
        for (int i = 0; i < HIDDEN_NODES; i++) {
            for (int j = 0; j < OUTPUT_NODES; j++) {
                CompoundTag wTag = new CompoundTag();
                wTag.putFloat("w", outputWeights[i][j]);
                outputWeightsList.add(wTag);
            }
        }
        tag.put("outputWeights", outputWeightsList);

        ListTag hiddenBiasList = new ListTag();
        for (int i = 0; i < HIDDEN_NODES; i++) {
            CompoundTag bTag = new CompoundTag();
            bTag.putFloat("b", hiddenBias[i]);
            hiddenBiasList.add(bTag);
        }
        tag.put("hiddenBias", hiddenBiasList);

        ListTag outputBiasList = new ListTag();
        for (int i = 0; i < OUTPUT_NODES; i++) {
            CompoundTag bTag = new CompoundTag();
            bTag.putFloat("b", outputBias[i]);
            outputBiasList.add(bTag);
        }
        tag.put("outputBias", outputBiasList);

        return tag;
    }

    public void load(CompoundTag tag) {
        trainingCount = tag.getInt("trainingCount");

        ListTag hiddenWeightsList = tag.getList("hiddenWeights", Tag.TAG_COMPOUND);
        int idx = 0;
        for (int i = 0; i < INPUT_NODES; i++) {
            for (int j = 0; j < HIDDEN_NODES; j++) {
                if (idx < hiddenWeightsList.size()) {
                    hiddenWeights[i][j] = hiddenWeightsList.getCompound(idx).getFloat("w");
                    idx++;
                }
            }
        }

        ListTag outputWeightsList = tag.getList("outputWeights", Tag.TAG_COMPOUND);
        idx = 0;
        for (int i = 0; i < HIDDEN_NODES; i++) {
            for (int j = 0; j < OUTPUT_NODES; j++) {
                if (idx < outputWeightsList.size()) {
                    outputWeights[i][j] = outputWeightsList.getCompound(idx).getFloat("w");
                    idx++;
                }
            }
        }

        ListTag hiddenBiasList = tag.getList("hiddenBias", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(HIDDEN_NODES, hiddenBiasList.size()); i++) {
            hiddenBias[i] = hiddenBiasList.getCompound(i).getFloat("b");
        }

        ListTag outputBiasList = tag.getList("outputBias", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(OUTPUT_NODES, outputBiasList.size()); i++) {
            outputBias[i] = outputBiasList.getCompound(i).getFloat("b");
        }
    }
}
