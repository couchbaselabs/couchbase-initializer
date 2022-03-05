'use strict'

const couchbase = require('couchbase')

async function main() {
  const cluster = await couchbase.connect('{{connectionString}}', {
    username: '{{username}}',
    password: '{{password}}',
  })

  // get a reference to our bucket
  const bucket = cluster.bucket('travel-sample')

  // get a reference to a collection
  const collection = bucket.scope('inventory').collection('airline')

  // get a reference to the default collection, required for older Couchbase server versions
  const collection_default = bucket.defaultCollection()

  const airline = {
    type: 'airline',
    id: 8091,
    callsign: 'CBS',
    iata: 'IATA',
    icao: 'ICAO',
    name: 'Couchbase Airways',
  }

  const upsertDocument = async (doc) => {
    try {
      // key will equal: "airline_8091"
      const key = `${doc.type}_${doc.id}`
      const result = await collection.upsert(key, doc)
      console.log('Upsert Result: ')
      console.log(result)
    } catch (error) {
      console.error(error)
    }
  }

  await upsertDocument(airline)

  const getAirlineByKey = async (key) => {
    try {
      const result = await collection.get(key)
      console.log('Get Result: ')
      console.log(result)
    } catch (error) {
      console.error(error)
    }
  }

  await getAirlineByKey('airline_8091')
}

// Run the main function
main()
  .catch((err) => {
    console.log('ERR:', err)
    process.exit(1)
  })
  .then(process.exit)
