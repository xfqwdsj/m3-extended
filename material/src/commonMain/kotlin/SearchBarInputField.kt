/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.ltfan.m3x.material

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults as MaterialSearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import top.ltfan.m3x.material.theme.extended

/** Theme-aware search field defaults. */
public object SearchBarDefaults {
    /** Shape of the collapsed search field. */
    public val inputFieldShape: Shape
        @Composable get() = MaterialTheme.extended.shapes.full

    /**
     * A text field to input a query in a search bar.
     *
     * This overload of [InputField] uses [TextFieldState] to keep track of the text content and
     * position of the cursor or selection, and [SearchBarState] to keep track of the state of the
     * search bar. It should be used with the search bar APIs which also accept a [SearchBarState].
     *
     * @param textFieldState [TextFieldState] that holds the internal editing state of the input
     *   field.
     * @param searchBarState the state of the search bar as a whole.
     * @param onSearch the callback to be invoked when the input service triggers the
     *   [ImeAction.Search] action. The current query in the [textFieldState] comes as a parameter
     *   of the callback.
     * @param modifier the [Modifier] to be applied to this input field.
     * @param enabled the enabled state of this input field. When `false`, this component will not
     *   respond to user input, and it will appear visually disabled and disabled to accessibility
     *   services.
     * @param readOnly controls the editable state of the input field. When `true`, the field cannot
     *   be modified. However, a user can focus it and copy text from it.
     * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
     * @param placeholder the placeholder to be displayed when the input text is empty.
     * @param leadingIcon the leading icon to be displayed at the start of the input field.
     * @param trailingIcon the trailing icon to be displayed at the end of the input field.
     * @param prefix the optional prefix to be displayed before the input text.
     * @param suffix the optional suffix to be displayed after the input text.
     * @param inputTransformation optional [InputTransformation] that will be used to transform
     *   changes to the [TextFieldState] made by the user. The transformation will be applied to
     *   changes made by hardware and software keyboard events, pasting or dropping text,
     *   accessibility services, and tests. The transformation will _not_ be applied when changing
     *   the [textFieldState] programmatically, or when the transformation is changed. If the
     *   transformation is changed on an existing text field, it will be applied to the next user
     *   edit. The transformation will not immediately affect the current [textFieldState].
     * @param outputTransformation optional [OutputTransformation] that transforms how the contents
     *   of the text field are presented.
     * @param scrollState scroll state that manages the horizontal scroll of the input field.
     * @param shape the shape of the input field.
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this input
     *   field in different states. See [MaterialSearchBarDefaults.inputFieldColors].
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this input field. You can use this to change the search bar's
     *   appearance or preview the search bar in different states. Note that if `null` is provided,
     *   interactions will still happen internally.
     * @param keyboardOptions software keyboard options that contains configuration such as
     *   [KeyboardType]. Note that the [ImeAction] will always be overwritten with
     *   [ImeAction.Search].
     * @param lineLimits whether the text field should be [TextFieldLineLimits.SingleLine], scroll
     *   horizontally, and ignore newlines; or [TextFieldLineLimits.MultiLine] and grow and scroll
     *   vertically.
     */
    @Composable
    public fun InputField(
        textFieldState: TextFieldState,
        searchBarState: SearchBarState,
        onSearch: (String) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        textStyle: TextStyle = LocalTextStyle.current,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        inputTransformation: InputTransformation? = null,
        outputTransformation: OutputTransformation? = null,
        scrollState: ScrollState = rememberScrollState(),
        shape: Shape = MaterialTheme.extended.shapes.full,
        colors: TextFieldColors = MaterialSearchBarDefaults.inputFieldColors(),
        interactionSource: MutableInteractionSource? = null,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    ): Unit =
        MaterialSearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = onSearch,
            modifier = modifier,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            inputTransformation = inputTransformation,
            outputTransformation = outputTransformation,
            scrollState = scrollState,
            shape = shape,
            colors = colors,
            interactionSource = interactionSource,
            keyboardOptions = keyboardOptions,
            lineLimits = lineLimits,
        )
}
