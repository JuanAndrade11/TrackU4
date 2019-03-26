package com.uninorte.edu.co.tracku;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

public class CuadroDialogo {

    public interface FinalizoCuadroDialogo{
        void ResultadoCuadroDialogo(String id, String Fini, String Ffin);
    }

    private FinalizoCuadroDialogo interfaz;
    public CuadroDialogo(Context context, FinalizoCuadroDialogo finalizoCuadroDialogo){

        interfaz = finalizoCuadroDialogo;
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.cuadro_dialogo);

        final EditText setId = (EditText) dialog.findViewById(R.id.set_Id);
        final EditText setIni = (EditText) dialog.findViewById(R.id.set_Ini);
        final EditText setFin = (EditText) dialog.findViewById(R.id.set_Fin);
        Button button = (Button) dialog.findViewById(R.id.button_Set);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                interfaz.ResultadoCuadroDialogo(setId.getText().toString(),setIni.getText().toString(),setFin.getText().toString());
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
