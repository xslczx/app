package com.xslczx.app.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textview.MaterialTextView;
import com.xslczx.app.AppInfo;
import com.xslczx.app.AppUtils;
import com.xslczx.app.DateUtil;
import com.xslczx.app.R;
import java.util.ArrayList;
import java.util.List;

public class RecycleViewAdapter extends RecyclerView.Adapter<RecycleViewAdapter.ViewHolder> {

  private final List<AppInfo> data = new ArrayList<>();
  private final List<String> mBatchList = new ArrayList<>();

  public void setNewData(List<AppInfo> list) {
    data.clear();
    data.addAll(list);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public RecycleViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View rowItem =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
    return new ViewHolder(rowItem);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    AppInfo appInfo = data.get(position);
    Context context = holder.itemView.getContext();
    if (!appInfo.isPackageInstalled(context)) {
      return;
    }
    holder.appIcon.setImageDrawable(appInfo.getIcon());

    holder.appDesc.setText(
        DateUtil.INSTANCE.timeStamp2DateStr(data.get(position).getLastUseDate()));
    holder.appName.setText(data.get(position).getLabel());
    holder.checkBox.setChecked(mBatchList.contains(data.get(position).getPackageName()));
    holder.appsize.setText(AppUtils.INSTANCE.size(appInfo.getSize()));
    holder.checkBox.setOnClickListener(v -> {
      if (!appInfo.isPackageInstalled(context)) {
        holder.checkBox.setChecked(false);
        return;
      }
      if (mBatchList.contains(data.get(position).getPackageName())) {
        mBatchList.remove(data.get(position).getPackageName());
      } else {
        mBatchList.add(data.get(position).getPackageName());
      }
    });
  }

  @Override
  public int getItemCount() {
    return data.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    private final AppCompatImageButton appIcon;
    private final MaterialCheckBox checkBox;
    private final MaterialTextView appName;
    private final MaterialTextView appDesc;
    private final MaterialTextView appsize;

    public ViewHolder(View view) {
      super(view);
      this.appIcon = view.findViewById(R.id.icon);
      this.appName = view.findViewById(R.id.title);
      this.appDesc = view.findViewById(R.id.description);
      this.checkBox = view.findViewById(R.id.checkbox);
      this.appsize = view.findViewById(R.id.size);
    }
  }
}