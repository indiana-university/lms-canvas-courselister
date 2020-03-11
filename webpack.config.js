/* global __dirname */
const webpack = require('webpack')
const path = require('path')
// const TerserPlugin = require('terser-webpack-plugin')

var packageJSON = require('./package.json');

const app = 'lms-lti-courselist'

const paths = {
  src: path.join(__dirname, '/src/main/js'),
  webapp: path.join(__dirname, '/src/main/webapp'),
  dest: path.join(__dirname, '/target/classes/META-INF/resources/jsreact', packageJSON.name),
  node: path.join(__dirname, '/node_modules'),
  prototype: path.join(__dirname, '/src/prototype'),
  devserver: path.join(__dirname, '/node_modules/webpack-dev-server')
}

module.exports = {
  context: paths.src,
  entry: ['babel-polyfill', 'index.js'],
  output: {
      filename: packageJSON.name + '.js',
      path: paths.dest
    },
  resolve: {
    alias: {
      src: paths.src
    },
    extensions: ['.js', '.jsx'],
    modules: [paths.src, paths.node]
  },
  resolveLoader: {
    modules: [paths.node]
  },
  mode: 'development',
  devtool: 'source-map',
  // optimization: {
  //   minimizer: [new TerserPlugin()],
  // },
  plugins: [
    new webpack.DefinePlugin({
      'process.env': {
        // NODE_ENV: JSON.stringify('production')
      }
    })
  ],
  devServer: {
    port: 8001,
    contentBase: [ paths.prototype, paths.webapp ],
    historyApiFallback: true
  },
  module: {
    rules: [
      {
        test: /\.(js)$/,
        include: [paths.src, paths.devserver],
        use: [
          {
            loader: 'babel-loader',
            options: {
              presets: ['react', 'env', 'stage-2']
            }
          }
        ]
      }, {
        test: /\.(less)$/,
        use: [
          {
            loader: 'file-loader',
            options: {
              name: `media/${app}.css`
            }
          },
          'postcss-loader',
          'less-loader'
        ]
      }, {
            test: /\.css$/,
            loaders: ['style-loader', 'css-loader']
      }, {
        test: /\.(html|gif|jpg|png)$/,
        use: [
          {
            loader: 'file-loader',
            options: {
              name: 'media/[name].[ext]'
            }
          }
        ]
      }, {
               test: /\.(svg)$/,
               use: [
                 {
                   loader: 'file-loader',
                   options: {
                     name: '[name].[ext]'
                   }
                 }
               ]
             }
    ]
  }
}