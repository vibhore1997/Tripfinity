package com.app.tripfinity.view;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.app.tripfinity.Adapter.ExpenseAdapter;
import com.app.tripfinity.R;
import com.app.tripfinity.model.Expense;
import com.app.tripfinity.model.User;
import com.app.tripfinity.viewmodel.ExpenseViewModel;
import com.app.tripfinity.viewmodel.MainExpenseViewModel;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExpenseActivity extends AppCompatActivity {
    private MainExpenseViewModel mainExpenseViewModel;
    private List<User> userList = new ArrayList<>();
    private List<Expense> expenseList = new ArrayList<>();
    private HashMap<String, Double> userAmountMap = new HashMap<>();
    private HashMap<String, String> userEmailToName = new HashMap<>();
    private ArrayList<String> dataToPopulate = new ArrayList<>();
    private double youOwe = 0;
    private double youAreOwed = 0;
    private String tripId = "77nrAgVzOA8xdm2wxPGa";
    private String loggedInUser = "abc@gmail.com";
    private TextView expenseUserName;
    private TextView expenseYouOwe;
    private TextView expenseYouAreOwed;
    private Button expenseHistory;
    private Button expenseAdd;
    private ExpenseAdapter expenseAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        expenseUserName = (TextView) findViewById(R.id.expenseUserName);
        expenseYouOwe = (TextView) findViewById(R.id.expenseYouOwe);
        expenseYouAreOwed = (TextView) findViewById(R.id.expenseYouAreOwed);
        expenseHistory = (Button) findViewById(R.id.expenseHistory);
        expenseAdd = (Button) findViewById(R.id.expenseAdd);
        initMainExpenseViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        userList = new ArrayList<>();
        expenseList = new ArrayList<>();
        userAmountMap = new HashMap<>();
        userEmailToName = new HashMap<>();
        dataToPopulate = new ArrayList<>();
        youOwe = 0;
        youAreOwed = 0;

        expenseUserName.setText("Hello " + "abc,");

        mainExpenseViewModel.getUserDataForTrip(tripId).observe(ExpenseActivity.this, list -> {
            Log.d("user list size in view ", String.valueOf(list.size()));
            userList = list;

            mainExpenseViewModel.getExpensesForTrip(tripId).observe(ExpenseActivity.this, expList -> {
                expenseList = expList;

                for (User user : userList) {
                    userEmailToName.put(user.getEmail(), user.getName());
                }

                // Explore all expenses
                for (Expense expense : expenseList) {

                    double eachSplit = expense.getAmount() / expense.getUserIds().size();
                    List<String> userIds = expense.getUserIds();

                    if (expense.getAddedByUser().equals(loggedInUser)) {
                        youAreOwed += eachSplit * (userIds.size() - 1);

                        for (String userEmail : userIds) {
                            if (!userEmail.equals(loggedInUser)) {
//                                userAmountMap.put(userEmail, userAmountMap.getOrDefault(userEmail, 0.0) + eachSplit);
                                if (userAmountMap.containsKey(userEmail)) {
                                    userAmountMap.put(userEmail, (double)userAmountMap.get(userEmail) + eachSplit);
                                } else {
                                    userAmountMap.put(userEmail, eachSplit);
                                }
                            }
                        }
                    } else {
                        if (userIds.contains(loggedInUser)) {
                            youOwe += eachSplit;

                            String currentPayer = expense.getAddedByUser();

//                            userAmountMap.put(currentPayer, userAmountMap.getOrDefault(currentPayer, 0.0) - eachSplit);
                            if (userAmountMap.containsKey(currentPayer)) {
                                userAmountMap.put(currentPayer, (double)userAmountMap.get(currentPayer) - eachSplit);
                            } else {
                                userAmountMap.put(currentPayer, (-1)*eachSplit);
                            }
                        }
                    }
                }

                // Processing done, Show all data here now
                expenseYouOwe.setText("You Owe $" + Math.round(youOwe * 100.0) / 100.0);
                expenseYouAreOwed.setText("You are Owed $" + Math.round(youAreOwed * 100.0) / 100.0);

                for (String email : userAmountMap.keySet()) {
                    if (userAmountMap.get(email) > 0) {
                        dataToPopulate.add(userEmailToName.get(email) + " owes you $" + Math.round(userAmountMap.get(email) * 100.0) / 100.0);
                    } else {
                        dataToPopulate.add("You owe " + userEmailToName.get(email) + "$" + Math.round(Math.abs(userAmountMap.get(email)) * 100.0) / 100.0);
                    }
                }

                createExpenseRecyclerView(dataToPopulate);

                expenseAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent myIntent = new Intent(ExpenseActivity.this, AddExpenseActivity.class);
                        startActivity(myIntent);
                    }
                });

            });
        });

    }

    private void createExpenseRecyclerView(ArrayList<String> dataToPopulate) {
        layoutManager = new LinearLayoutManager(this);
        RecyclerView expenseRecyclerView = findViewById(R.id.expenseRecyclerView);
        expenseRecyclerView.setHasFixedSize(true);

        expenseAdapter = new ExpenseAdapter(dataToPopulate);
        expenseRecyclerView.setAdapter(expenseAdapter);
        expenseRecyclerView.setLayoutManager(layoutManager);
    }

    @Override
    protected void onStop() {
        super.onStop();
        userList = new ArrayList<>();
        expenseList = new ArrayList<>();
        userAmountMap = new HashMap<>();
        userEmailToName = new HashMap<>();
        dataToPopulate = new ArrayList<>();
        youOwe = 0;
        youAreOwed = 0;
    }

    private void initMainExpenseViewModel() {
        mainExpenseViewModel = new ViewModelProvider(this).get(MainExpenseViewModel.class);
    }

}