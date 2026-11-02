package org.example.authentication.domain

import android.content.Context
import android.content.Intent
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.example.authentication.R

/**
 * The kind of account a user signs up as. Each type is implemented by its own
 * Gradle module (:personal, :business, :organization).
 *
 * :authentication cannot depend on those modules (they depend on it), so the
 * Activity is referenced by class name and resolved at runtime; all modules are
 * merged into the same app, so the lookup always succeeds.
 */
enum class AccountType(
    val activityClassName: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val signUpTitle: Int,
    @StringRes val setupTitle: Int,
    @StringRes val nameLabel: Int,
    @DrawableRes val icon: Int,
    @ColorRes val accentColor: Int,
    /** Large illustration shown on the account type selection card. */
    @DrawableRes val illustration: Int,
    /** Label of the industry / organization-type field; null when the type has no extra details. */
    @StringRes val industryLabel: Int? = null,
    @StringRes val registrationLabel: Int? = null,
) {
    PERSONAL(
        activityClassName = "org.example.personal.PerRegForm",
        title = R.string.authentication_account_personal,
        description = R.string.authentication_account_personal_desc,
        signUpTitle = R.string.authentication_title_sign_up_personal,
        setupTitle = R.string.authentication_title_setup_personal,
        nameLabel = R.string.authentication_label_name_personal,
        icon = R.drawable.authentication_ic_personal,
        accentColor = R.color.authentication_accent_personal,
        illustration = R.drawable.authentication_illus_personal,
    ),
    BUSINESS(
        activityClassName = "org.example.business.BusRegForm",
        title = R.string.authentication_account_business,
        description = R.string.authentication_account_business_desc,
        signUpTitle = R.string.authentication_title_sign_up_business,
        setupTitle = R.string.authentication_title_setup_business,
        nameLabel = R.string.authentication_label_name_business,
        icon = R.drawable.authentication_ic_business,
        accentColor = R.color.authentication_accent_business,
        illustration = R.drawable.authentication_illus_business,
        industryLabel = R.string.authentication_label_industry_business,
        registrationLabel = R.string.authentication_label_registration_business,
    ),
    ORGANIZATION(
        activityClassName = "org.example.organization.OrgRegForm",
        title = R.string.authentication_account_organization,
        description = R.string.authentication_account_organization_desc,
        signUpTitle = R.string.authentication_title_sign_up_organization,
        setupTitle = R.string.authentication_title_setup_organization,
        nameLabel = R.string.authentication_label_name_organization,
        icon = R.drawable.authentication_ic_organization,
        accentColor = R.color.authentication_accent_organization,
        illustration = R.drawable.authentication_illus_organization,
        industryLabel = R.string.authentication_label_industry_organization,
        registrationLabel = R.string.authentication_label_registration_organization,
    );

    /** Business and Organization collect industry, phone and registration ID at sign-up. */
    val hasDetails: Boolean get() = industryLabel != null

    /** Intent for this type's Activity (sign-up form when signed out, home when signed in). */
    fun intent(context: Context): Intent = Intent().setClassName(context, activityClassName)

    companion object {
        /** Unknown / missing values (e.g. rows created before account types existed) are PERSONAL. */
        fun fromName(name: String?): AccountType = entries.firstOrNull { it.name == name } ?: PERSONAL
    }
}
