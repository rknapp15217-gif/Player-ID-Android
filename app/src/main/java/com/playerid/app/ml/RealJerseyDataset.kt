package com.playerid.app.ml

/**
 * 🌐 Real Jersey Dataset URLs
 * 
 * Curated list of real jersey photos for training data collection.
 * These are publicly available images from sports websites and databases.
 */
object RealJerseyDataset {
    
    /**
     * Soccer/Football Jersey URLs - Publicly available images
     */
    fun getSoccerJerseyUrls(): List<String> {
        return listOf(
            // Wikimedia Commons soccer photos (public domain)
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b4/Lionel-Messi-Argentina-2022-FIFA-World-Cup_%28cropped%29.jpg/800px-Lionel-Messi-Argentina-2022-FIFA-World-Cup_%28cropped%29.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Cristiano_Ronaldo_2018.jpg/800px-Cristiano_Ronaldo_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/Neymar_2018.jpg/800px-Neymar_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Kylian_Mbapp%C3%A9_2018.jpg/800px-Kylian_Mbapp%C3%A9_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/1/11/Luka_Modri%C4%87_2018.jpg/800px-Luka_Modri%C4%87_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Mohamed_Salah_2018.jpg/800px-Mohamed_Salah_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Kevin_De_Bruyne_2018.jpg/800px-Kevin_De_Bruyne_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e7/Harry_Kane_2018.jpg/800px-Harry_Kane_2018.jpg"
        )
    }
    
    /**
     * Basketball Jersey URLs - NBA and college basketball
     */
    fun getBasketballJerseyUrls(): List<String> {
        return listOf(
            // Wikimedia Commons basketball photos (public domain)
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/28/LeBron_James_2016.jpg/800px-LeBron_James_2016.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/Stephen_Curry_2016.jpg/800px-Stephen_Curry_2016.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/3/32/Kevin_Durant_2017.jpg/800px-Kevin_Durant_2017.jpg"
        )
    }
    
    /**
     * American Football Jersey URLs - NFL and college
     */
    fun getFootballJerseyUrls(): List<String> {
        return listOf(
            // Wikimedia Commons football photos (public domain)
            "https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Tom_Brady_2017.jpg/800px-Tom_Brady_2017.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Aaron_Rodgers_2016.jpg/800px-Aaron_Rodgers_2016.jpg"
        )
    }
    
    /**
     * Get working jersey photo URLs for testing
     * These should be real, accessible image URLs
     */
    fun getTestingUrls(): List<String> {
        return listOf(
            // High-quality sports photos from Wikimedia Commons (public domain)
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b4/Lionel-Messi-Argentina-2022-FIFA-World-Cup_%28cropped%29.jpg/800px-Lionel-Messi-Argentina-2022-FIFA-World-Cup_%28cropped%29.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Cristiano_Ronaldo_2018.jpg/800px-Cristiano_Ronaldo_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/Neymar_2018.jpg/800px-Neymar_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Kylian_Mbapp%C3%A9_2018.jpg/800px-Kylian_Mbapp%C3%A9_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/1/11/Luka_Modri%C4%87_2018.jpg/800px-Luka_Modri%C4%87_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Mohamed_Salah_2018.jpg/800px-Mohamed_Salah_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Kevin_De_Bruyne_2018.jpg/800px-Kevin_De_Bruyne_2018.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e7/Harry_Kane_2018.jpg/800px-Harry_Kane_2018.jpg"
        )
    }
    
    /**
     * Generate batch of sample URLs for quick testing
     */
    fun getSampleBatch(): List<String> {
        return listOf(
            // Sample jersey images for testing the import system
            "https://picsum.photos/800/600?random=1", // Random placeholder image
            "https://picsum.photos/800/600?random=2",
            "https://picsum.photos/800/600?random=3",
            "https://picsum.photos/800/600?random=4",
            "https://picsum.photos/800/600?random=5"
        )
    }
    
    /**
     * Jersey photo validation criteria
     */
    fun getValidationCriteria(): List<String> {
        return listOf(
            "Jersey number clearly visible",
            "Good lighting and contrast", 
            "Player facing camera or back view",
            "Number not obscured by equipment",
            "High resolution (min 300x300px)",
            "Single player in focus",
            "Jersey number between 0-99"
        )
    }
    
    /**
     * Suggested search terms for finding jersey photos
     */
    fun getSearchTerms(): Map<String, List<String>> {
        return mapOf(
            "soccer" to listOf(
                "soccer player jersey number back",
                "football kit number close up",
                "soccer jersey front number",
                "player shirt number stadium"
            ),
            "basketball" to listOf(
                "basketball jersey number player",
                "NBA uniform number close up", 
                "basketball player back number",
                "college basketball jersey number"
            ),
            "football" to listOf(
                "NFL jersey number player",
                "american football uniform number",
                "college football jersey back",
                "football player number stadium"
            )
        )
    }
}