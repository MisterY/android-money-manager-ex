/*
 * Copyright (C) 2012-2015 The Android Money Manager Ex Project Team
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
package com.money.manager.ex.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.money.manager.ex.Constants;
import com.money.manager.ex.R;
import com.money.manager.ex.database.MoneyManagerOpenHelper;
import com.money.manager.ex.database.QueryAllData;
import com.money.manager.ex.database.QueryBillDeposits;
import com.money.manager.ex.database.TransactionStatus;
import com.money.manager.ex.utils.CurrencyUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 *
 */
@SuppressLint("UseSparseArrays")
public class AllDataAdapter
        extends CursorAdapter {
    // type cursor
    private TypeCursor mTypeCursor = TypeCursor.ALLDATA;

    // define cursor field
    private String ID, DATE, ACCOUNTID, STATUS, AMOUNT, TRANSACTIONTYPE, TOACCOUNTID, TOTRANSAMOUNT,
            CURRENCYID, PAYEE, ACCOUNTNAME, TOACCOUNTNAME, CATEGORY, SUBCATEGORY, NOTES, TOCURRENCYID;
    private LayoutInflater mInflater;
    // hash map for group
    private HashMap<Integer, Integer> mHeadersAccountIndex;
    // private HashMap<Integer, Double> mBalanceTransactions;
    private SparseBooleanArray mCheckedPosition;
    // account and currency
    private int mAccountId = -1;
    private int mCurrencyId = -1;
    // show account name and show balance
    private boolean mShowAccountName = false;
    private boolean mShowBalanceAmount = false;
    // database for balance account
    private SQLiteDatabase mDatabase;
    // core and context
    private Context mContext;

    public AllDataAdapter(Context context, Cursor c, TypeCursor typeCursor) {
        super(context, c, -1);

        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // create hash map
        mHeadersAccountIndex = new HashMap<>();
        // create sparse array boolean checked
        mCheckedPosition = new SparseBooleanArray();

        mTypeCursor = typeCursor;

        mContext = context;

        setFieldFromTypeCursor();
    }

    @SuppressWarnings({})
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // take a holder
        AllDataViewHolder holder = (AllDataViewHolder) view.getTag();
        // header index
        int accountId = cursor.getInt(cursor.getColumnIndex(ACCOUNTID));
        if (!mHeadersAccountIndex.containsKey(accountId)) {
            mHeadersAccountIndex.put(accountId, cursor.getPosition());
        }
        // write status
        String status = cursor.getString(cursor.getColumnIndex(STATUS));
        holder.txtStatus.setText(TransactionStatus.getStatusAsString(mContext, status));
        // color status
        int colorBackground = TransactionStatus.getBackgroundColorFromStatus(mContext, status);
        holder.linDate.setBackgroundColor(colorBackground);
        holder.txtStatus.setTextColor(Color.GRAY);
        // date group
        try {
            Locale locale = mContext.getResources().getConfiguration().locale;

            Date date = new SimpleDateFormat(Constants.PATTERN_DB_DATE, locale)
                    .parse(cursor.getString(cursor.getColumnIndex(DATE)));
            holder.txtMonth.setText(new SimpleDateFormat("MMM", locale).format(date));
            holder.txtYear.setText(new SimpleDateFormat("yyyy", locale).format(date));
            holder.txtDay.setText(new SimpleDateFormat("dd", locale).format(date));
        } catch (ParseException e) {
            Log.e(AllDataAdapter.class.getSimpleName(), e.getMessage());
        }
        // take transaction amount
        double amount = cursor.getDouble(cursor.getColumnIndex(AMOUNT));
        // set currency id
        setCurrencyId(cursor.getInt(cursor.getColumnIndex(CURRENCYID)));

        // manage transfer and change amount sign
        String transactionType = cursor.getString(cursor.getColumnIndex(TRANSACTIONTYPE));
        boolean isTransfer = Constants.TRANSACTION_TYPE_TRANSFER.equalsIgnoreCase(transactionType);
        if ((transactionType != null) && isTransfer) {
            if (getAccountId() != cursor.getInt(cursor.getColumnIndex(TOACCOUNTID))) {
                amount = -(amount); // -total
            } else if (getAccountId() == cursor.getInt(cursor.getColumnIndex(TOACCOUNTID))) {
                amount = cursor.getDouble(cursor.getColumnIndex(TOTRANSAMOUNT)); // to account = account
                setCurrencyId(cursor.getInt(cursor.getColumnIndex(TOCURRENCYID)));
            }
        }

        // check amount sign
        CurrencyUtils currencyUtils = new CurrencyUtils(mContext);
        holder.txtAmount.setText(currencyUtils.getCurrencyFormatted(getCurrencyId(), amount));
        // text color amount
        if (Constants.TRANSACTION_TYPE_TRANSFER.equalsIgnoreCase(cursor.getString(cursor.getColumnIndex(TRANSACTIONTYPE)))) {
            holder.txtAmount.setTextColor(mContext.getResources().getColor(R.color.material_grey_700));
        } else if (Constants.TRANSACTION_TYPE_DEPOSIT.equalsIgnoreCase(cursor.getString(cursor.getColumnIndex(TRANSACTIONTYPE)))) {
            holder.txtAmount.setTextColor(mContext.getResources().getColor(R.color.material_green_700));
        } else {
            holder.txtAmount.setTextColor(mContext.getResources().getColor(R.color.material_red_700));
        }

        // compose payee description
        holder.txtPayee.setText(cursor.getString(cursor.getColumnIndex(PAYEE)));
        // compose account name
        if (isShowAccountName()) {
            if (mHeadersAccountIndex.containsValue(cursor.getPosition())) {
                holder.txtAccountName.setText(cursor.getString(cursor.getColumnIndex(ACCOUNTNAME)));
                holder.txtAccountName.setVisibility(View.VISIBLE);
            } else {
                holder.txtAccountName.setVisibility(View.GONE);
            }
        } else {
            holder.txtAccountName.setVisibility(View.GONE);
        }

        // write ToAccountName
        String toAccountName = "";
        if ((!TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(TOACCOUNTNAME))))) {
            if (getAccountId() != cursor.getInt(cursor.getColumnIndex(TOACCOUNTID))) {
                toAccountName = cursor.getString(cursor.getColumnIndex(TOACCOUNTNAME));
            } else {
                toAccountName = cursor.getString(cursor.getColumnIndex(ACCOUNTNAME));
            }
            toAccountName = "[%]".replace("%", toAccountName);
            holder.txtPayee.setText(toAccountName);
        }

        // compose category description
        String categorySub = cursor.getString(cursor.getColumnIndex(CATEGORY));
        // check sub category
        if (!(TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(SUBCATEGORY))))) {
            categorySub += " : <i>" + cursor.getString(cursor.getColumnIndex(SUBCATEGORY)) + "</i>";
        }
        // write category/subcategory format html
        if (!TextUtils.isEmpty(categorySub)) {
            // Display category/sub-category.
            holder.txtCategorySub.setText(Html.fromHtml(categorySub));
        } else {
            // It is either a Transfer or a split category.
            if (isTransfer) {
                holder.txtCategorySub.setText(R.string.transfer);
            } else {
                // then it is a split? todo: improve this check to make it explicit.
                holder.txtCategorySub.setText(R.string.split_category);
            }
        }

        // notes
        if (!TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(NOTES)))) {
            holder.txtNotes.setText(Html.fromHtml("<small>" + cursor.getString(cursor.getColumnIndex(NOTES)) + "</small>"));
            holder.txtNotes.setVisibility(View.VISIBLE);
        } else {
            holder.txtNotes.setVisibility(View.GONE);
        }
        // check if item is checked
        if (mCheckedPosition.get(cursor.getPosition(), false)) {
            view.setBackgroundResource(R.color.material_green_100);
        } else {
            view.setBackgroundResource(android.R.color.transparent);
        }
        // balance account or days left
        if (mTypeCursor == TypeCursor.ALLDATA) {
            if (isShowBalanceAmount() && getDatabase() != null) {
                int transId = cursor.getInt(cursor.getColumnIndex(ID));
                // create thread for calculate balance amount
                BalanceAmountTask balanceAmount = new BalanceAmountTask();
                balanceAmount.setAccountId(getAccountId());
                balanceAmount.setDate(cursor.getString(cursor.getColumnIndex(DATE)));
                balanceAmount.setTextView(holder.txtBalance);
                balanceAmount.setContext(mContext);
                balanceAmount.setDatabase(getDatabase());
                balanceAmount.setCurrencyId(getCurrencyId());
                balanceAmount.setTransId(transId);
                // execute thread
                balanceAmount.execute();
            } else {
                holder.txtBalance.setVisibility(View.GONE);
            }
        } else {
            int daysLeft = cursor.getInt(cursor.getColumnIndex(QueryBillDeposits.DAYSLEFT));
            if (daysLeft == 0) {
                holder.txtBalance.setText(R.string.due_today);
            } else {
                holder.txtBalance.setText(Integer.toString(Math.abs(daysLeft)) + " " + context.getString(daysLeft > 0 ? R.string.days_remaining : R.string.days_overdue));
            }
            holder.txtBalance.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.item_alldata_account, parent, false);
        // holder
        AllDataViewHolder holder = new AllDataViewHolder();
        // take a pointer of object UI
        holder.linDate = (LinearLayout) view.findViewById(R.id.linearLayoutDate);
        holder.txtDay = (TextView) view.findViewById(R.id.textViewDay);
        holder.txtMonth = (TextView) view.findViewById(R.id.textViewMonth);
        holder.txtYear = (TextView) view.findViewById(R.id.textViewYear);
        holder.txtStatus = (TextView) view.findViewById(R.id.textViewStatus);
        holder.txtAmount = (TextView) view.findViewById(R.id.textViewAmount);
        holder.txtPayee = (TextView) view.findViewById(R.id.textViewPayee);
        holder.txtAccountName = (TextView) view.findViewById(R.id.textViewAccountName);
        holder.txtCategorySub = (TextView) view.findViewById(R.id.textViewCategorySub);
        holder.txtNotes = (TextView) view.findViewById(R.id.textViewNotes);
        holder.txtBalance = (TextView) view.findViewById(R.id.textViewBalance);
        // set holder to view
        view.setTag(holder);

        return view;
    }

    public void clearPositionChecked() {
        mCheckedPosition.clear();
    }

    /**
     * Set checked in position
     */
    public void setPositionChecked(int position, boolean checked) {
        mCheckedPosition.put(position, checked);
    }

    /**
     * @return the mAccountId
     */
    public int getAccountId() {
        return mAccountId;
    }

    /**
     * @param mAccountId the mAccountId to set
     */
    public void setAccountId(int mAccountId) {
        this.mAccountId = mAccountId;
    }

    /**
     * @return the mCurrencyId
     */
    public int getCurrencyId() {
        return mCurrencyId;
    }

    /**
     * @param mCurrencyId the mCurrencyId to set
     */
    public void setCurrencyId(int mCurrencyId) {
        this.mCurrencyId = mCurrencyId;
    }

    /**
     * @return the mShowAccountName
     */
    public boolean isShowAccountName() {
        return mShowAccountName;
    }

    public void resetAccountHeaderIndexes() {
        mHeadersAccountIndex.clear();
    }

    /**
     * @param showAccountName the mShowAccountName to set
     */
    public void setShowAccountName(boolean showAccountName) {
        this.mShowAccountName = showAccountName;
    }

    /**
     * @return the mDatabase
     */
    public SQLiteDatabase getDatabase() {
        if (mDatabase == null) {
            mDatabase = MoneyManagerOpenHelper.getInstance(mContext.getApplicationContext())
                    .getReadableDatabase();
        }

        return mDatabase;
    }

    /**
     * @param mDatabase the mDatabase to set
     */
    public void setDatabase(SQLiteDatabase mDatabase) {
        this.mDatabase = mDatabase;
    }

    /**
     * @return the mShowBalanceAmount
     */
    public boolean isShowBalanceAmount() {
        return mShowBalanceAmount;
    }

    /**
     * @param mShowBalanceAmount the mShowBalanceAmount to set
     */
    public void setShowBalanceAmount(boolean mShowBalanceAmount) {
        this.mShowBalanceAmount = mShowBalanceAmount;
    }

    public void setFieldFromTypeCursor() {
        ID = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.ID : QueryBillDeposits.BDID;
        DATE = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.Date : QueryBillDeposits.NEXTOCCURRENCEDATE;
        ACCOUNTID = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.ACCOUNTID : QueryBillDeposits.ACCOUNTID;
        STATUS = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.Status : QueryBillDeposits.STATUS;
        AMOUNT = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.Amount : QueryBillDeposits.AMOUNT;
        TRANSACTIONTYPE = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.TransactionType : QueryBillDeposits.TRANSCODE;
        TOACCOUNTID = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.ToAccountID : QueryBillDeposits.TOACCOUNTID;
        TOTRANSAMOUNT = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.TOTRANSAMOUNT : QueryBillDeposits.TOTRANSAMOUNT;
        CURRENCYID = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.CURRENCYID : QueryBillDeposits.CURRENCYID;
        TOCURRENCYID = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.ToCurrencyID : QueryBillDeposits.CURRENCYID;
        PAYEE = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.Payee : QueryBillDeposits.PAYEENAME;
        ACCOUNTNAME = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.AccountName : QueryBillDeposits.ACCOUNTNAME;
        TOACCOUNTNAME = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.ToAccountName : QueryBillDeposits.TOACCOUNTNAME;
        CATEGORY = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.Category : QueryBillDeposits.CATEGNAME;
        SUBCATEGORY = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.Subcategory : QueryBillDeposits.SUBCATEGNAME;
        NOTES = mTypeCursor == TypeCursor.ALLDATA ? QueryAllData.Notes : QueryBillDeposits.NOTES;
    }

    // source type: AllData or RepeatingTransaction
    public enum TypeCursor {
        ALLDATA, REPEATINGTRANSACTION
    }
}
