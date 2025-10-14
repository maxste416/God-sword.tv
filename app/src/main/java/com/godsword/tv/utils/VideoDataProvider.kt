package com.godsword.tv.utils

import com.godsword.tv.models.Category
import com.godsword.tv.models.Video

object VideoDataProvider {
    
    fun getCategories(): List<Category> {
        return listOf(
            Category("Worship Videos", getWorshipVideos()),
            Category("Teachings", getTeachingVideos()),
            Category("Gospel Films", getGospelFilms()),
            Category("Kids Corner", getKidsVideos()),
            Category("Bible Stories", getBibleStories()),
            Category("Testimonies", getTestimonies())
        )
    }
    
    private fun getWorshipVideos() = listOf(
        Video(
            "w1",
            "Amazing Grace - Live Worship",
            "Beautiful worship session featuring Amazing Grace",
            "12:45",
            "https://picsum.photos/400/300?random=1",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "Worship"
        ),
        Video(
            "w2",
            "How Great Thou Art",
            "Classic hymn performed by worship team",
            "8:30",
            "https://picsum.photos/400/300?random=2",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            "Worship"
        ),
        Video(
            "w3",
            "Cornerstone - Hillsong",
            "Powerful worship experience",
            "10:15",
            "https://picsum.photos/400/300?random=3",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "Worship"
        )
    )
    
    private fun getTeachingVideos() = listOf(
        Video(
            "t1",
            "Walking in Faith",
            "Pastor John teaches about living by faith",
            "45:20",
            "https://picsum.photos/400/300?random=4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "Teaching"
        ),
        Video(
            "t2",
            "The Power of Prayer",
            "Understanding effective prayer",
            "38:15",
            "https://picsum.photos/400/300?random=5",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            "Teaching"
        )
    )
    
    private fun getGospelFilms() = listOf(
        Video(
            "g1",
            "The Gospel Story",
            "A visual journey through the life of Jesus",
            "95:30",
            "https://picsum.photos/400/300?random=6",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            "Gospel Films"
        ),
        Video(
            "g2",
            "Grace Redeemed",
            "A modern parable about forgiveness",
            "82:45",
            "https://picsum.photos/400/300?random=7",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            "Gospel Films"
        )
    )
    
    private fun getKidsVideos() = listOf(
        Video(
            "k1",
            "David and Goliath",
            "Animated story for children",
            "15:20",
            "https://picsum.photos/400/300?random=8",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            "Kids"
        ),
        Video(
            "k2",
            "Noah's Ark Adventure",
            "Fun animated Bible story",
            "18:30",
            "https://picsum.photos/400/300?random=9",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
            "Kids"
        )
    )
    
    private fun getBibleStories() = listOf(
        Video(
            "b1",
            "The Good Samaritan",
            "Jesus' parable brought to life",
            "22:10",
            "https://picsum.photos/400/300?random=10",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            "Bible Stories"
        )
    )
    
    private fun getTestimonies() = listOf(
        Video(
            "ts1",
            "From Darkness to Light",
            "A powerful testimony of transformation",
            "28:45",
            "https://picsum.photos/400/300?random=11",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4",
            "Testimonies"
        )
    )
}
