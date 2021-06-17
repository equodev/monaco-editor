/****************************************************************************
**
** Copyright (C) 2021 Equo
**
** This file is part of Equo Framework.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equoplatform.com/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/

const path = require('path');
const lib = path.resolve(__dirname, "lib");
const resources = path.resolve(__dirname, "../resources/");
const MonacoWebpackPlugin = require('../../com.equo.eclipse.monaco.editor/node_modules/monaco-editor-webpack-plugin');

const common = {
    entry: {
        "index": path.resolve(lib, "index.js"),
    },
    output: {
        filename: '[name].bundle.js',
        path: resources
    },
    module: {
        rules: [{
            test: /\.css$/,
            use: ['style-loader', 'css-loader']
        }, {
		  test: /\.ttf$/,
		  use: ['file-loader']
		}]
    },
    target: 'web',
    node: {
        fs: 'empty',
        child_process: 'empty',
        net: 'empty',
        crypto: 'empty'
    },
    resolve: {
        modules: [path.resolve(__dirname, "../../com.equo.eclipse.monaco.editor/node_modules/"), "node_modules"],
        alias: {
            'vscode': require.resolve('../../com.equo.eclipse.monaco.editor/node_modules/monaco-languageclient/lib/vscode-compatibility')
        },
        extensions: ['.js', '.ttf']
    },
    plugins: [
        new MonacoWebpackPlugin()
    ]
};

module.exports = common;
