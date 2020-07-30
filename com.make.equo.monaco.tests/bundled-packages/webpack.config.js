const path = require('path');
const lib = path.resolve(__dirname, "lib");
const resources = path.resolve(__dirname, "../resources/");
const MonacoWebpackPlugin = require('../../com.make.equo.monaco/node_modules/monaco-editor-webpack-plugin');

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
        modules: [path.resolve(__dirname, "../../com.make.equo.monaco/node_modules/"), "node_modules"],
        alias: {
            'vscode': require.resolve('../../com.make.equo.monaco/node_modules/monaco-languageclient/lib/vscode-compatibility')
        },
        extensions: ['.js', '.ttf']
    },
    plugins: [
        new MonacoWebpackPlugin()
    ]
};

module.exports = common;
