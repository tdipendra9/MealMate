package edu.ismt.dipendra.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import edu.ismt.dipendra.mealmate.R;

public class InstructionStepAdapter extends RecyclerView.Adapter<InstructionStepAdapter.ViewHolder> {

    private final Context context;
    private final List<String> instructionSteps;
    private final OnStepClickListener listener;

    public InstructionStepAdapter(Context context, List<String> instructionSteps, OnStepClickListener listener) {
        this.context = context;
        this.instructionSteps = instructionSteps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.instruction_step_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String step = instructionSteps.get(position);
        holder.textViewStepNumber.setText(String.valueOf(position + 1));
        holder.textViewStepInstruction.setText(step);
        
        holder.buttonDeleteStep.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStepDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return instructionSteps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewStepNumber;
        TextView textViewStepInstruction;
        MaterialButton buttonDeleteStep;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewStepNumber = itemView.findViewById(R.id.textViewStepNumber);
            textViewStepInstruction = itemView.findViewById(R.id.textViewStepInstruction);
            buttonDeleteStep = itemView.findViewById(R.id.buttonDeleteStep);
        }
    }

    public interface OnStepClickListener {
        void onStepDelete(int position);
    }
} 