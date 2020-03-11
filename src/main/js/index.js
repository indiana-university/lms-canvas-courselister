import React from 'react'
import ReactDOM from 'react-dom'
import { injectGlobal } from 'styled-components'

import App from 'App'

ReactDOM.render(
  <App />,
  document.getElementById('root')
)

// Leaving this here, in case it's needed for something in the future
// eslint-disable-next-line
//injectGlobal`
//  body {
//  }
//`

if (module.hot) {
  module.hot.accept(App)
}
