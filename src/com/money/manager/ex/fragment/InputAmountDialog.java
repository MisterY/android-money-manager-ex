/*
 * Copyright (C) 2012-2014 Alessandro Lazzari
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.money.manager.ex.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.money.manager.ex.Constants;
import com.money.manager.ex.R;
import com.money.manager.ex.utils.CurrencyUtils;
import com.money.manager.ex.utils.MathUtils;

import org.apache.commons.lang3.math.NumberUtils;

public class InputAmountDialog extends DialogFragment {
    private static final String LOGCAT = InputAmountDialog.class.getSimpleName();

    private static final String KEY_ID_VIEW = "InputAmountDialog:Id";
    private static final String KEY_AMOUNT = "InputAmountDialog:Amount";
    private static final String KEY_CURRENCY_ID = "InputAmountDialog:CurrencyId";
    private static final String COMMA_DECIMAL = ".";

    // arrays id keynum button
    private int[] idButtonKeyNum = {R.id.buttonKeyNum0, R.id.buttonKeyNum1, R.id.buttonKeyNum2, R.id.buttonKeyNum3,
            R.id.buttonKeyNum4, R.id.buttonKeyNum5, R.id.buttonKeyNum6, R.id.buttonKeyNum7, R.id.buttonKeyNum8,
            R.id.buttonKeyNum9, R.id.buttonKeyNumDecimal};

    private int mIdView;
    private String mAmount = Constants.EMPTY_STRING;
    private Integer mCurrencyId;
    private TextView txtAmount;
    private ImageButton imgDelete;

    public static InputAmountDialog getInstance(int id, Double amount) {
        Bundle args = new Bundle();
        args.putInt("id", id);
        args.putDouble("amount", amount);

        InputAmountDialog fragment = new InputAmountDialog();
        fragment.setArguments(args);

        return fragment;
    }

    public static InputAmountDialog getInstance(int id, Double amount, Integer currencyId) {
        InputAmountDialog dialog = getInstance(id, amount);
        dialog.mCurrencyId = currencyId;
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            int id = getArguments().getInt("id");
            Double amount = MathUtils.Round(getArguments().getDouble("amount"), 2);
            mIdView = id;
            if (!(amount == null || amount == 0)) {
                int iAmount = (int) (amount * 100);
                if (Math.abs(amount - (iAmount / 100)) == 0) {
                    mAmount = Integer.toString(iAmount / 100);
                } else {
                    mAmount = Double.toString(amount);
                }
            }
        } else {
            if (savedInstanceState.containsKey(KEY_AMOUNT))
                mAmount = savedInstanceState.getString(KEY_AMOUNT);
            if (savedInstanceState.containsKey(KEY_CURRENCY_ID))
                mCurrencyId = savedInstanceState.getInt(KEY_CURRENCY_ID);
            if (savedInstanceState.containsKey(KEY_ID_VIEW))
                mIdView = savedInstanceState.getInt(KEY_ID_VIEW);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        View view = inflater.inflate(R.layout.input_amount_dialog, null);
        // create listener
        OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                String parseAmountTry = mAmount;
                parseAmountTry += ((Button) v).getText();
                // check if amount string is empty
                if (TextUtils.isEmpty(parseAmountTry))
                    parseAmountTry = Double.toString(0);
                // check if amount end with
                // check if amount is number
                if (NumberUtils.isNumber(parseAmountTry)) {
                    // change amount
                    mAmount = parseAmountTry;
                    refreshAmount();
                }
            }
        };
        // reference button click listener
        for (int id : idButtonKeyNum) {
            Button button = (Button) view.findViewById(id);
            button.setOnClickListener(clickListener);
        }
        // image button delete
        imgDelete = (ImageButton) view.findViewById(R.id.imageButtonCancel);
        imgDelete.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(mAmount)) {
                    if (mAmount.endsWith(COMMA_DECIMAL)) {
                        mAmount = mAmount.substring(0, mAmount.length() - 2);
                    } else {
                        mAmount = mAmount.substring(0, mAmount.length() - 1);
                    }
                }
                refreshAmount();
            }
        });
        // reference TextView amount
        txtAmount = (TextView) view.findViewById(R.id.textViewAmount);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.customView(view, false);
        builder.cancelable(false);
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                if (TextUtils.isEmpty(mAmount))
                    mAmount = Double.toString(0);
                // check if is double
                if (NumberUtils.isNumber(mAmount)) {
                    ((InputAmountDialogListener) getActivity()).onFinishedInputAmountDialog(mIdView, MathUtils.Round(Double.parseDouble(mAmount), 2));
                    dismiss();
                }
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                dismiss();
            }
        });
        builder.negativeText(android.R.string.cancel);
        builder.positiveText(android.R.string.ok);

        Dialog dialog = builder.show();
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAmount();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(KEY_AMOUNT, mAmount);
        if (mCurrencyId != null)
            savedInstanceState.putInt(KEY_CURRENCY_ID, mCurrencyId);
        savedInstanceState.putInt(KEY_ID_VIEW, mIdView);
    }

    public void refreshAmount() {
        String amount = mAmount;
        // check if amount is not empty and is double
        if (TextUtils.isEmpty(amount))
            amount = Double.toString(0);

        if (NumberUtils.isNumber(amount)) {
            double fAmount = Double.parseDouble(amount);

            CurrencyUtils currencyUtils = new CurrencyUtils(getActivity());

            if (mCurrencyId == null) {
                txtAmount.setText(currencyUtils.getBaseCurrencyFormatted(fAmount));
            } else {
                txtAmount.setText(currencyUtils.getCurrencyFormatted(mCurrencyId, fAmount));
            }
        }
    }

    public interface InputAmountDialogListener {
        public void onFinishedInputAmountDialog(int id, Double amount);
    }
}
