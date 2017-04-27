package cz.zdrubecky.criminalintent;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.io.File;


public class ImageEnlargeFragment extends DialogFragment {
    private static final String ARG_URI = "uri";

    public static ImageEnlargeFragment newInstance(File file) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_URI, file);

        ImageEnlargeFragment fragment = new ImageEnlargeFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        File file = (File) getArguments().getSerializable(ARG_URI);

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_image, null);

        ImageView imageView = (ImageView) view.findViewById(R.id.dialog_image_view);
        Bitmap bitmap = PictureUtils.getScaledBitmap(file.getPath(), getActivity());

        imageView.setImageBitmap(bitmap);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.enlarged_image_dialog)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
