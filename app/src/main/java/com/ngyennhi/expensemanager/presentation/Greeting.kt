package com.ngyennhi.expensemanager.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.greeting, stringResource(R.string.greeting_name)),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    PersonalExpenseManagerTheme {
        Greeting()
    }
}
