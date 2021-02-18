/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.extension.vim_switch;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.key.OperatorFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.maddyhome.idea.vim.extension.VimExtensionFacade.*;
import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author dhleong
 */
public class SwitchExtension implements VimExtension {
  @Override
  public @NotNull String getName() {
    return "switch";
  }

  @Override
  public void init() {
    putExtensionHandlerMapping(MappingMode.N, parseKeys("<Plug>(VimSwitch)"), getOwner(), new VimSwitchHandler(), false);

    putKeyMappingIfMissing(MappingMode.N, parseKeys("gs"), getOwner(), parseKeys("<Plug>(VimSwitch)"), true);
  }

  private static class VimSwitchHandler implements VimExtensionHandler {
    @Override
    public boolean isRepeatable() {
      return true;
    }

    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {
      // always use line-wise comments
      if (!new Operator().apply(editor, context, SelectionType.LINE_WISE)) {
        return;
      }

      WriteAction.run(() -> {
        // Leave visual mode
        executeNormalWithoutMapping(parseKeys("<Esc>"), editor);
        editor.getCaretModel().moveToOffset(editor.getCaretModel().getPrimaryCaret().getSelectionStart());
      });
    }
  }

  private static class Operator implements OperatorFunction {
    String[][] g_switch_custom_definitions = new String[][]{
      {"true", "false"},
      {"false", "true"},
      {"True", "False"},
      {"False", "True"},

      {"public", "private"},
      {"private", "public"},
      {"static ", ""},

      {"pick", "fixup"},
      {"fixup", "reword"},
      {"reword", "edit"},
      {"edit", "squash"},
      {"squash", "exec"},
      {"exec", "break"},
      {"break", "drop"},
      {"drop", "label"},
      {"label", "reset"},
      {"reset", "merge"},
      {"merge", "pick"},

      {"p", "fixup"},
      {"f", "reword"},
      {"r", "edit"},
      {"e", "squash"},
      {"s", "exec"},
      {"x", "break"},
      {"b", "drop"},
      {"d", "label"},
      {"l", "reset"},
      {"t", "merge"},
      {"m", "pick"}
    };

    @Nullable
    public static int[] extractWordFrom(@NotNull String text, int index) {
      int length = text.length();
      if (length <= 0 || index < 0 || index > length) {
        return null;
      }
      int begin = index;
      while (begin > 0 && Character.isJavaIdentifierPart(text.charAt(begin - 1))) {
        begin--;
      }
      int end = index;
      while (end < length && Character.isJavaIdentifierPart(text.charAt(end))) {
        end++;
      }
      if (end <= begin || (begin == 0 && end == length) ) {
        return null;
      }
      return new int[]{begin, end};
    }

    public static String currentWord(Document document, int start, int end) {
      return document.getText(TextRange.create(start, end));
    }

    @Override
    public boolean apply(@NotNull Editor editor, @NotNull DataContext context, @NotNull SelectionType selectionType) {
      return WriteAction.compute(() -> {
        try {

          int[] startEnd;

          Document document = editor.getDocument();

          if (editor.getCaretModel().getPrimaryCaret().hasSelection()) {
            Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
            startEnd = new int[] {primaryCaret.getSelectionStart(), primaryCaret.getSelectionEnd()};
          } else {
            startEnd = extractWordFrom(document.getText(), editor.getCaretModel().getOffset());
          }

          String currentText = currentWord(document, startEnd[0], startEnd[1]);

          for (String[] def: g_switch_custom_definitions) {
            if (currentText.matches(def[0])) {
              document.replaceString(startEnd[0], startEnd[1], def[1]);
              break;
            }
          }

          return true;
        } finally {
          editor.getSelectionModel().removeSelection();
        }
      });
    }
  }
}
