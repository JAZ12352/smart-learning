import request from '@/utils/request'

const evaluationApi = {
  getProgress: function (userId) {
    return request({
      url: '/api/admin/evaluation/progress/' + userId,
      method: 'get'
    })
  },
  generateReport: function (userId, data, onChunk) {
    var token = localStorage.getItem('token') || ''
    return fetch('/api/admin/evaluation/report/' + userId, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      },
      credentials: 'include',
      body: JSON.stringify(data)
    }).then(function (response) {
      if (!response.ok) {
        throw new Error('HTTP error! status: ' + response.status)
      }
      var reader = response.body.getReader()
      var decoder = new TextDecoder('utf-8')
      var completeContent = ''

      function read () {
        return reader.read().then(function (result) {
          if (result.done) return completeContent
          var chunk = decoder.decode(result.value, { stream: true })
          completeContent += chunk
          if (onChunk) {
            onChunk(chunk, completeContent)
          }
          return read()
        })
      }

      return read()
    })
  }
}

export default evaluationApi
